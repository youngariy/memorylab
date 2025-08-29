package com.memorylab.web.auth;

import com.memorylab.dto.auth.AuthDtos.*;
import com.memorylab.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;   // ★ 추가
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @Value("${app.auth.verify-base-url}")   // ★ 주입
    private String verifyBaseUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterReq req){
        auth.register(req, verifyBaseUrl);  // ★ 하드코딩 대신 설정값 사용
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
        return ResponseEntity.ok(java.util.Map.of("accessToken", access));
    }
}
