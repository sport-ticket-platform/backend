package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyRequest(
        @NotBlank(message = "LOGIN_MFA_REQUIRED")
        @Size(max = 255, message = "LOGIN_MFA_SIZE")
        String mfa,

        @NotBlank(message = "LOGIN_OTP_REQUIRED")
        @Size(max = 6, message = "LOGIN_OTP_SIZE")
        String otp
) {
}
