package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h3>Login Request Data Transfer Object</h3>
 *
 * @author logTAHA
 */
@Builder
public record LoginRequest (
        @NotBlank(message = "LOGIN_USERNAME_REQUIRED")
        @Size(min = 3, max = 65, message = "LOGIN_USERNAME_SIZE")
        String username,

        @NotBlank(message = "LOGIN_PASSWORD_REQUIRED")
        @Size(max = 32, message = "LOGIN_PASSWORD_SIZE")
        String password
){
}