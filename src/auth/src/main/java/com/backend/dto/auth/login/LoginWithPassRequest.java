package com.backend.dto.auth.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h3>Login Request Data Transfer Object</h3>
 *
 * @author logTAHA
 */
@Builder
public record LoginWithPassRequest(
        @NotBlank(message = "LOGIN_IDENTIFIER_REQUIRED")
        @Size(min = 3, max = 255, message = "LOGIN_IDENTIFIER_SIZE")
        String identifier,

        @NotBlank(message = "LOGIN_PASSWORD_REQUIRED")
        @Size(max = 32, message = "LOGIN_PASSWORD_SIZE")
        String password
){
}