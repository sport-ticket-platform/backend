package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SignupRequest (

        @NotBlank(message = "SIGNUP_USERNAME_REQUIRED")
        @Size(min = 3, max = 65, message = "SIGNUP_USERNAME_SIZE")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "SIGNUP_USERNAME_FORMAT")
        String username,

        @NotBlank(message = "SIGNUP_FIRSTNAME_REQUIRED")
        @Size(min = 2, max = 50, message = "SIGNUP_FIRSTNAME_SIZE")
        @Pattern(regexp = "^[\\p{L}\\s]+$", message = "SIGNUP_NAME_FORMAT")
        String first_name,

        @NotBlank(message = "SIGNUP_LASTNAME_REQUIRED")
        @Size(min = 2, max = 50, message = "SIGNUP_LASTNAME_SIZE")
        @Pattern(regexp = "^[\\p{L}\\s]+$", message = "SIGNUP_NAME_FORMAT")
        String last_name,

        @NotBlank(message = "SIGNUP_PASSWORD_REQUIRED")
        @Size(min = 8, max = 32, message = "SIGNUP_PASSWORD_SIZE")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?]+$", message = "SIGNUP_PASSWORD_WEAK")
        String password

) {
}