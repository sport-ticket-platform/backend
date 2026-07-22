package com.backend.dto.auth.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginOTPEmailRequest(
        @NotBlank(message = "LOGIN_EMAIL_REQUIRED")
        @Size(min = 3, max = 255, message = "LOGIN_EMAIL_SIZE")
        String email
) {
}
