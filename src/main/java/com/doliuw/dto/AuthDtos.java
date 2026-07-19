package com.doliuw.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ─── Auth DTOs ────────────────────────────────────────────────

public class AuthDtos {

    @Data
    public static class EmailSignupRequest {
        @NotBlank private String name;
        @Email @NotBlank private String email;
        @NotBlank @Size(min = 6, max = 100) private String password;
    }

    @Data
    public static class EmailLoginRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    public static class SendOtpRequest {
        @NotBlank
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        private String mobile;
        private String name; // optional – used during signup
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank private String mobile;
        @NotBlank @Size(min = 6, max = 6) private String otp;
        private String name; // used during signup to set name
    }

    @Data
    public static class GoogleCallbackRequest {
        @NotBlank private String sessionId;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private UserDto user;

        public AuthResponse(String token, UserDto user) {
            this.token = token;
            this.user = user;
        }
    }

    @Data
    public static class UserDto {
        private Long id;
        private String name;
        private String email;
        private String mobile;
        private String avatar;
        private String provider;
    }
}
