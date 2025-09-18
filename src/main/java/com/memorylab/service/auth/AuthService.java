package com.memorylab.service.auth;

import com.memorylab.domain.user.VerificationCode;
import com.memorylab.repository.user.VerificationCodeRepository;
import com.memorylab.common.util.JwtProvider;
import com.memorylab.domain.RefreshToken;
import com.memorylab.domain.user.User;
import com.memorylab.dto.auth.AuthDtos.*;
import com.memorylab.repository.RefreshTokenRepository;
import com.memorylab.repository.user.UserRepository;
import com.memorylab.service.event.VerificationEmailEvent;
import com.memorylab.service.mail.EmailService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final VerificationCodeRepository verificationCodes;
    private final RefreshTokenRepository refreshTokens;
    private final EmailService mail;
    private final JwtProvider jwt;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public void checkEmailAvailability(String email) {
        if (users.existsByEmail(email.trim().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
    }

    @Transactional(readOnly = true)
    public void checkNicknameAvailability(String nickname) {
        if (users.existsByNickname(nickname)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }
    }

    @Transactional
    public void sendVerificationCode(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        checkEmailAvailability(email);

        // 쿨다운 검사
        verificationCodes.findByEmail(email).ifPresent(existingCode -> {
            if (Duration.between(existingCode.getLastSentAt(), LocalDateTime.now()).getSeconds() < 60) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "너무 많은 요청: 60초 후에 다시 시도해주세요.");
            }
        });

        String code = createVerificationCode();

        // JPA 기본 기능을 사용한 UPSERT 로직
        VerificationCode verificationCode = verificationCodes.findByEmail(email)
                .map(existingCode -> {
                    existingCode.updateCode(code);
                    return existingCode;
                })
                .orElseGet(() -> new VerificationCode(email, code));
        
        verificationCodes.save(verificationCode);

        // 메일 발송은 이벤트로 위임
        eventPublisher.publishEvent(new VerificationEmailEvent(email, code));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationEmail(VerificationEmailEvent event) {
        try {
            mail.send(event.email(), "[MemoryLab] 이메일 인증 코드", "인증 코드는 [" + event.code() + "] 입니다. 5분 내에 입력해주세요.");
        } catch (Exception e) {
            log.error("Verification email send failed for: {}", event.email(), e);
        }
    }

    public void verifyEmailCode(String rawEmail, String code) {
        String email = rawEmail.trim().toLowerCase();
        VerificationCode verificationCode = verificationCodes.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인증 코드가 발송되지 않은 이메일입니다."));

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationCodes.delete(verificationCode);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다.");
        }
        if (!verificationCode.getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다.");
        }
        verificationCode.setVerified(true);
        verificationCodes.save(verificationCode);
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

        User user = User.builder()
                .email(email)
                .password(encoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .name(req.getName())
                .emailVerified(true)
                .roles(List.of("ROLE_USER"))
                .build();
        users.save(user);

        verificationCodes.delete(vCode);
    }

    private String createVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public LoginRes login(LoginReq req){
        var u = users.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 자격 증명"));

        if (!u.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이메일 미인증");
        }

        if (!encoder.matches(req.getPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 자격 증명");
        }

        String accessToken = jwt.createAccessToken(u.getId(), u.getEmail(), u.getRoles());
        String refreshTokenValue = jwt.createRefreshToken(u.getId(), u.getEmail());

        Claims claims = Jwts.parser().verifyWith(jwt.getKey()).build().parseSignedClaims(refreshTokenValue).getPayload();
        Instant expiryDate = claims.getExpiration().toInstant();

        refreshTokens.findByUserId(u.getId())
                .ifPresentOrElse(
                        (token) -> token.updateToken(refreshTokenValue, expiryDate),
                        () -> refreshTokens.save(RefreshToken.builder()
                                .userId(u.getId())
                                .tokenValue(refreshTokenValue)
                                .expiryDate(expiryDate)
                                .build())
                );

        return LoginRes.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileRes getProfile(Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        var u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        return ProfileRes.builder()
                .id(u.getId())
                .email(u.getEmail())
                .nickname(u.getNickname())
                .name(u.getName())
                .emailVerified(u.isEmailVerified())
                .roles(u.getRoles())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public RefreshRes refreshAccessToken(RefreshReq req) {
        String tokenValue = req.getRefreshToken();

        var refreshToken = refreshTokens.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokens.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwt.getKey())
                    .build()
                    .parseSignedClaims(tokenValue)
                    .getPayload();

            Long userId = claims.get("uid", Long.class);

            User user = users.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found for refresh token"));

            String newAccessToken = jwt.createAccessToken(user.getId(), user.getEmail(), user.getRoles());

            return RefreshRes.builder()
                    .accessToken(newAccessToken)
                    .build();

        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }
}
