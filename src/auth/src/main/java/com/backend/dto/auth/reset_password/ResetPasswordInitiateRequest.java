package com.backend.dto.auth.reset_password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ResetPasswordInitiateRequest(

        @NotBlank(message = "SIGNUP_EMAIL_REQUIRED")
        @Email(message = "SIGNUP_EMAIL_FORMAT")
        String email
) {
}