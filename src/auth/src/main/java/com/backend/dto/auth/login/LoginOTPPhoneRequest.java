package com.backend.dto.auth.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginOTPPhoneRequest(
        @NotBlank(message = "LOGIN_PHONE_REQUIRED")
        @Pattern(regexp = "^(?:\\+98|0098|0)?9\\d{9}$", message = "LOGIN_PHONE_SIZE")
        String phone
) {
}
