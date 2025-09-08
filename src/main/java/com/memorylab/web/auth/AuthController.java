package com.memorylab.web.auth;

import com.memorylab.dto.auth.AuthDtos.*;
import com.memorylab.service.auth.AuthService;
import com.memorylab.domain.user.User;   // ← User 엔티티 import
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ★ 추가
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @Value("${app.auth.verify-base-url}")
    private String verifyBaseUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterReq req){
        auth.register(req, verifyBaseUrl);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token){
        auth.verifyEmail(token);
        return ResponseEntity.ok("이메일 인증 완료");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginReq req){
        var access = auth.login(req);
        return ResponseEntity.ok(Map.of("accessToken", access));
    }

    // ✅ 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ProfileRes> me(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(auth.getProfile(userId));
    }
}
