package com.backend.dto.auth.login;

import lombok.*;

/**
 * <h3>Authentication Response Data Transfer Object</h3>
 * <p>
 * Contains the generated <strong>JWT</strong> token to be returned to the authenticated client.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Builder
public record LoginResponse(
        String step,             // "SUCCESS" or "2FA-EMAIL" or "2FA-PHONE"
        String refresh_token,     // Null in 2fa
        String mfa_token          // Null in success
) {}