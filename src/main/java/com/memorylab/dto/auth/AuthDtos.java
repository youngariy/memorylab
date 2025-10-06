package com.memorylab.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class AuthDtos {

    @Getter
    @NoArgsConstructor // for JSON deserialization
    public static class RegisterReq {
        @Email @NotBlank private String email;
        @NotBlank private String password;
        @NotBlank private String nickname;
        @NotBlank private String name;
    }

    @Getter
    public static class LoginReq {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfileRes {
        private Long id;
        private String email;
        private String nickname;
        private String name;
        private boolean emailVerified;
        private List<String> roles;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LoginRes {
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshReq {
        @NotBlank private String refreshToken;
    }

    // public record RefreshRes(String accessToken) {} // 이전 코드
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor // public 생성자를 만들어주기 위해 추가
    public static class RefreshRes {
        private String accessToken;
    }


    // === DTOs for new signup flow ===
    @Getter
    @NoArgsConstructor
    public static class SendVerificationCodeReq {
        @Email @NotBlank private String email;
    }

    @Getter
    @NoArgsConstructor
    public static class VerifyCodeReq {
        @Email @NotBlank private String email;
        @NotBlank private String code;
    }
}
