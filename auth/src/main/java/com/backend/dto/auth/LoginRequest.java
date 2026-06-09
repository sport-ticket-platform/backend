package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h3>Login Request Data Transfer Object</h3>
 * <p>
 * Encapsulates the credentials provided by the client attempting to authenticate.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Builder
public record LoginRequest (
        @NotBlank(message = "Username can't be empty")
        @Size(min = 3, max = 65, message = "Username must be between 3-65")
        String username,

        @NotBlank(message = "Username can't be empty")
        @Size(max = 32, message = "Password length should be less than 32")
        String password
){

}
