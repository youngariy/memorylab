package com.memorylab.service.auth;

import com.memorylab.common.util.HashUtils;
import com.memorylab.config.jwt.JwtProvider;
import com.memorylab.domain.RefreshToken;
import com.memorylab.domain.user.Member;
import com.memorylab.domain.user.MemberRepository;
import com.memorylab.domain.user.VerificationCode;
import com.memorylab.dto.auth.AuthDtos.*;
import com.memorylab.repository.RefreshTokenRepository;
import com.memorylab.repository.user.VerificationCodeRepository;
import com.memorylab.service.event.VerificationEmailEvent;
import com.memorylab.service.mail.EmailService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final VerificationCodeRepository verificationCodes;
    private final RefreshTokenRepository refreshTokens;
    private final EmailService mail;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoginRes login(LoginReq req) {
        Member member = memberRepository.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 자격 증명"));

        if (!member.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이메일 미인증");
        }
        if (!encoder.matches(req.getPassword(), member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 자격 증명");
        }

        String accessToken = jwtProvider.createAccessToken(member);
        var newRefreshToken = jwtProvider.createRefreshTokenWithMeta(member);

        // 리프레시 토큰 정보를 DB에 저장 (해시하여 저장)
        refreshTokens.save(RefreshToken.of(
                newRefreshToken.jti(),
                member.getId(),
                HashUtils.sha256(newRefreshToken.token()),
                newRefreshToken.expiresAt()
        ));

        return new LoginRes(accessToken, newRefreshToken.token());
    }

    @Transactional
    public RefreshRes refreshAccessToken(RefreshReq req) {
        final String token = req.getRefreshToken();
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is missing.");
        }

        try {
            Claims claims = jwtProvider.parser()
                    .require("typ", "refresh") // 이 토큰은 반드시 refresh 타입이어야 함
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            String email = claims.getSubject();

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            RefreshToken rt = refreshTokens.findByJti(jti)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not recognized"));

            if (rt.isRevoked() || rt.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
            }

            if (!HashUtils.slowEquals(rt.getTokenHash(), HashUtils.sha256(token))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token mismatch");
            }

            // 토큰 회전(Rotation): 기존 토큰 폐기 및 새 토큰 발급
            rt.setRevoked(true);
            refreshTokens.save(rt);

            String newAccessToken = jwtProvider.createAccessToken(member);
            var newRefreshToken = jwtProvider.createRefreshTokenWithMeta(member);
            refreshTokens.save(RefreshToken.of(
                    newRefreshToken.jti(),
                    member.getId(),
                    HashUtils.sha256(newRefreshToken.token()),
                    newRefreshToken.expiresAt()
            ));

            return new RefreshRes(newAccessToken);

        } catch (JwtException e) {
            log.warn("Invalid JWT received for refresh: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.");
        } catch (ResponseStatusException e) {
            throw e; // 이미 올바른 HTTP 상태 코드를 가진 예외는 그대로 던짐
        }
    }

    // --- 나머지 기존 메소드들 ---
    @Transactional(readOnly = true)
    public void checkEmailAvailability(String email) {
        if (memberRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
    }

    @Transactional(readOnly = true)
    public void checkNicknameAvailability(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }
    }

    @Transactional
    public void sendVerificationCode(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        checkEmailAvailability(email);

        verificationCodes.findByEmail(email).ifPresent(existingCode -> {
            if (Duration.between(existingCode.getLastSentAt(), LocalDateTime.now()).getSeconds() < 60) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "60초 후에 다시 시도해주세요.");
            }
        });

        String code = createVerificationCode();
        VerificationCode verificationCode = verificationCodes.findByEmail(email)
                .map(c -> { c.updateCode(code); return c; })
                .orElseGet(() -> new VerificationCode(email, code));
        verificationCodes.save(verificationCode);
        eventPublisher.publishEvent(new VerificationEmailEvent(email, code));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationEmail(VerificationEmailEvent event) {
        try {
            mail.send(event.email(), "[MemoryLab] 이메일 인증 코드", "인증 코드는 [" + event.code() + "] 입니다.");
        } catch (Exception e) {
            log.error("Verification email send failed for: {}", event.email(), e);
        }
    }

    public void verifyEmailCode(String rawEmail, String code) {
        String email = rawEmail.trim().toLowerCase();
        VerificationCode vCode = verificationCodes.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 코드가 발송되지 않았습니다."));

        if (vCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationCodes.delete(vCode);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다.");
        }
        if (!vCode.getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다.");
        }
        vCode.setVerified(true);
        verificationCodes.save(vCode);
    }

    @Transactional
    public void register(RegisterReq req) {
        String email = req.getEmail().trim().toLowerCase();
        checkEmailAvailability(email);
        checkNicknameAvailability(req.getNickname());

        VerificationCode vCode = verificationCodes.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."));
        if (!vCode.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다.");
        }

        Member member = Member.builder()
                .email(email)
                .password(encoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .name(req.getName())
                .emailVerified(true)
                .roles(List.of("ROLE_USER"))
                .build();
        memberRepository.save(member);
        verificationCodes.delete(vCode);
    }

    private String createVerificationCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    @Transactional(readOnly = true)
    public ProfileRes getProfile(Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        return ProfileRes.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .name(member.getName())
                .emailVerified(member.isEmailVerified())
                .roles(member.getRoles())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
