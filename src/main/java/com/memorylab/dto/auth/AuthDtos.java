package com.memorylab.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

public class AuthDtos {

    @Getter
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
}
