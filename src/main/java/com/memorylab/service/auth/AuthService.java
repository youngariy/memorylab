package com.memorylab.service.auth;

import com.memorylab.common.util.JwtProvider;
import com.memorylab.domain.user.EmailToken;
import com.memorylab.domain.user.User;
import com.memorylab.dto.auth.AuthDtos.*;
import com.memorylab.repository.user.EmailTokenRepository;
import com.memorylab.repository.user.UserRepository;
import com.memorylab.service.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service @RequiredArgsConstructor @Transactional
public class AuthService {

    private final UserRepository users;
    private final EmailTokenRepository tokens;
    private final EmailService mail;
    private final JwtProvider jwt;
    private final PasswordEncoder encoder;

    // 회원가입 + 인증메일 발송
    public void register(RegisterReq req, String verifyBaseUrl){
        if (users.existsByEmail(req.getEmail())) throw new IllegalArgumentException("이미 사용 중인 이메일");
        if (users.existsByNickname(req.getNickname())) throw new IllegalArgumentException("이미 사용 중인 닉네임");

        var u = users.save(User.builder()
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .name(req.getName())
                .emailVerified(false)
                .build());

        var token = tokens.save(EmailToken.issue(u, EmailToken.TokenType.VERIFY, 60));
        var link = verifyBaseUrl + "?token=" + token.getToken();
//        mail.send(u.getEmail(), "MemoryLab 이메일 인증", "아래 링크로 인증하세요:\n" + link);
    }

    public void verifyEmail(String token){
        var t = tokens.findByToken(token).orElseThrow(() -> new IllegalArgumentException("토큰 없음"));
        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) throw new IllegalArgumentException("토큰 만료/사용됨");
        var u = t.getUser();
        u.setEmailVerified(true);
        t.setUsed(true);
    }

    @Transactional(readOnly = true)
    public String login(LoginReq req){
        var u = users.findByEmail(req.getEmail()).orElseThrow(() -> new IllegalArgumentException("계정 없음"));
        if (!u.isEmailVerified()) throw new IllegalStateException("이메일 미인증");
        if (!encoder.matches(req.getPassword(), u.getPassword())) throw new IllegalArgumentException("비밀번호 불일치");
        return jwt.createAccessToken(u.getId(), u.getEmail());
    }
}
