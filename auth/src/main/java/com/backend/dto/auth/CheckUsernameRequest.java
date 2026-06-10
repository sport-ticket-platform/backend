package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CheckUsernameRequest (
        @NotBlank(message = "SIGNUP_USERNAME_REQUIRED")
        @Size(min = 3, max = 65, message = "SIGNUP_USERNAME_SIZE")
        String username
){

}
