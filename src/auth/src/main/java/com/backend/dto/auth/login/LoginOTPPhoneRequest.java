package com.backend.dto.auth.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginOTPPhoneRequest(
        @NotBlank(message = "LOGIN_PHONE_REQUIRED")
        @Size(min = 10, max = 14, message = "LOGIN_PHONE_SIZE")
        String phone
) {
}
