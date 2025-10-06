package com.memorylab.web.auth;

import com.memorylab.dto.auth.AuthDtos.*;
import com.memorylab.service.auth.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated // RequestParam 유효성 검사를 위해 추가
public class AuthController {
    private final AuthService auth;

    // --- New Endpoints for Signup Flow ---

    @GetMapping("/check-email")
    public ResponseEntity<Void> checkEmail(@RequestParam @Email @NotBlank String email) {
        auth.checkEmailAvailability(email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Void> checkNickname(@RequestParam @NotBlank String nickname) {
        auth.checkNicknameAvailability(nickname);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<Void> sendVerificationCode(@RequestBody @Valid SendVerificationCodeReq req) {
        auth.sendVerificationCode(req.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Void> verifyCode(@RequestBody @Valid VerifyCodeReq req) {
        auth.verifyEmailCode(req.getEmail(), req.getCode());
        return ResponseEntity.ok().build();
    }

    // --- Modified Register Endpoint ---
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterReq req) {
        auth.register(req);
        return ResponseEntity.ok().build();
    }

    // --- Existing Endpoints (login, refresh, me) ---

    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@RequestBody @Valid LoginReq req) {
        LoginRes tokens = auth.login(req);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshRes> refresh(@RequestBody @Valid RefreshReq req) {
        RefreshRes res = auth.refreshAccessToken(req);
        return ResponseEntity.ok(res);
    }

//    @GetMapping("/me")
//    public ResponseEntity<ProfileRes> me(@AuthenticationPrincipal Long userId) {
//        return ResponseEntity.ok(auth.getProfile(userId));
//    }
}
