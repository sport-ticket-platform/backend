package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SignupRequest (

        @NotBlank(message = "SIGNUP_USERNAME_REQUIRED")
        @Size(min = 3, max = 65, message = "SIGNUP_USERNAME_SIZE")
        String username,

        @NotBlank(message = "SIGNUP_FIRSTNAME_REQUIRED")
        @Size(min = 2, max = 50, message = "SIGNUP_FIRSTNAME_SIZE")
        String first_name,

        @NotBlank(message = "SIGNUP_LASTNAME_REQUIRED")
        @Size(min = 2, max = 50, message = "SIGNUP_LASTNAME_SIZE")
        String last_name,

        @NotBlank(message = "SIGNUP_PASSWORD_REQUIRED")
        @Size(min = 8, max = 32, message = "SIGNUP_PASSWORD_SIZE")
        String password

) {
}