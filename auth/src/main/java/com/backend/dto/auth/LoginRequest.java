package com.backend.dto.auth;

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
        String username,
        String password
){

}
