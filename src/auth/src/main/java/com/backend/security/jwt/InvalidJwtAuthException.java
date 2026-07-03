package com.backend.security.jwt;

import org.springframework.security.core.AuthenticationException;

/**
 * <h2><span style="color:red;">Invalid JWT Exception</span></h2>
 * <p>Thrown when a JWT token is invalid.</p>
 * <p>Extends <code>AuthenticationException</code> so related to Spring Security.</p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
public class InvalidJwtAuthException extends AuthenticationException {
    public InvalidJwtAuthException(String message) {
        super(message);
    }

    public InvalidJwtAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
