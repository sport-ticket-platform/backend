package com.backend.dto.auth.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SignupInitiateRequest(

        @NotBlank(message = "SIGNUP_EMAIL_REQUIRED")
        @Email(message = "SIGNUP_EMAIL_FORMAT")
        String email
) {
}