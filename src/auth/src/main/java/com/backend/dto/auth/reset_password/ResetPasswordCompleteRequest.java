package com.backend.dto.auth.reset_password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ResetPasswordCompleteRequest(

        @NotBlank(message = "SIGNUP_TEMP_TOKEN_REQUIRED")
        String temp_token,

        @NotBlank(message = "SIGNUP_PASSWORD_REQUIRED")
        @Size(min = 8, max = 32, message = "SIGNUP_PASSWORD_SIZE")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?]+$", message = "SIGNUP_PASSWORD_WEAK")
        String password
) {
}