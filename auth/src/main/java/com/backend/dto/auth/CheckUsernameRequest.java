package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CheckUsernameRequest (
        @NotBlank(message = "SIGNUP_USERNAME_REQUIRED")
        @Size(min = 3, max = 65, message = "SIGNUP_USERNAME_SIZE")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "SIGNUP_USERNAME_FORMAT")
        String username
){

}
