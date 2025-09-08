package com.memorylab.service.auth;

import com.memorylab.common.util.JwtProvider;
import com.memorylab.domain.user.EmailToken;
import com.memorylab.domain.user.User;
import com.memorylab.dto.auth.AuthDtos.*;
import com.memorylab.repository.user.EmailTokenRepository;
import com.memorylab.repository.user.UserRepository;
import com.memorylab.service.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository users;
    private final EmailTokenRepository tokens;
    private final EmailService mail;
    private final JwtProvider jwt;
    private final PasswordEncoder encoder;

    // 회원가입 + 인증메일 발송
    public void register(RegisterReq req, String verifyBaseUrl){
        if (users.existsByEmail(req.getEmail())) {
            // 409: 중복 리소스
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일");
        }
        if (users.existsByNickname(req.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임");
        }

        var u = users.save(User.builder()
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .name(req.getName())
                .emailVerified(false)
                .build());

        var token = tokens.save(EmailToken.issue(u, EmailToken.TokenType.VERIFY, 60));
        var link = verifyBaseUrl + "?token=" + token.getToken();
        // mail.send(u.getEmail(), "MemoryLab 이메일 인증", "아래 링크로 인증하세요:\n" + link);
    }

    public void verifyEmail(String token){
        var t = tokens.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "토큰 없음"));
        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            // 400: 클라이언트가 잘못된/만료된 토큰을 보냄
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "토큰 만료/사용됨");
        }
        var u = t.getUser();
        u.setEmailVerified(true);
        t.setUsed(true);
    }

    @Transactional(readOnly = true)
    public String login(LoginReq req){
        var u = users.findByEmail(req.getEmail())
                // 존재/비번 불일치를 구분 안 하고 401로 통일하면 보안상 더 안전
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 자격 증명"));

        if (!u.isEmailVerified()) {
            // 이메일 미인증 → 403(금지). 필요하면 401로 통일해도 됨.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이메일 미인증");
        }

        if (!encoder.matches(req.getPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 자격 증명");
        }

        return jwt.createAccessToken(u.getId(), u.getEmail());
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
                .createdAt(u.getCreatedAt())
                .build();
    }
}
