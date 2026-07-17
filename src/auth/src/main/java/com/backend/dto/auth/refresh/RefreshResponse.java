package com.backend.dto.auth.refresh;

import lombok.Builder;

/**
 * Response returned after a successful refresh token request.
 *
 * <p>This application uses the <b>Refresh Token Rotation</b> strategy.
 * Every successful refresh operation issues both a new access token and a
 * brand-new single-use refresh token.</p>
 *
 * <ul>
 *   <li>
 *     <b>access_token</b> — A newly generated JWT used to access protected APIs.
 *   </li>
 *   <li>
 *     <b>refresh_token</b> — A newly generated single-use refresh token.
 *     The previously issued refresh token is immediately revoked and can no
 *     longer be used.
 *   </li>
 * </ul>
 *
 * <p>If a revoked refresh token is used again, it is treated as a potential
 * token compromise (refresh token replay attack). In this case, all active
 * user sessions are invalidated to protect the account.</p>
 *
 * @param refresh_token the newly issued single-use refresh token
 * @param access_token the newly generated JWT access token
 */
@Builder
public record RefreshResponse(
        String refresh_token,
        String access_token
) {}