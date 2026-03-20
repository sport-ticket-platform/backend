package com.backend.dto.auth;

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
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String token;
}
