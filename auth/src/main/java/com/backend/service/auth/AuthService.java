package com.backend.service.auth;

import com.backend.dto.auth.LoginRequest;
import com.backend.dto.auth.LoginResponse;
import com.backend.entity.User;
import com.backend.handler.UserSuspendException;
import com.backend.security.jwt.JwtTokenProvider;
import com.backend.security.userdetails.CustomUserDetails;
import com.backend.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * <h2>Authentication Service</h2>
 * <p>
 * Handles the core business logic for user authentication operations.
 * It interacts with the <code>AuthenticationManager</code> to validate credentials
 * and delegates token generation to the <code>JwtTokenProvider</code>.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    private final RefreshTokenService refreshTokenService;

    /**
     * Authenticates a user based on the provided credentials and generates a JWT.
     *
     * @param loginRequest the DTO containing the username and raw password.
     * @return an {@link LoginResponse} containing the generated JWT.
     */
    public LoginResponse login(LoginRequest loginRequest, String ip, String userAgent, String deviceId) {
        log.info("Attempting to authenticate user: {}", loginRequest.username());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );
        }
        catch (BadCredentialsException | CredentialsExpiredException e) {
            log.warn("Authentication failed for user {}: {}", loginRequest.username(), e.getMessage());
            throw e;
        }
        catch (AuthenticationException e) {
            log.error("Authentication error for user {}: {}", loginRequest.username(), e.getMessage());
            throw new IllegalStateException("Authentication error occurred", e);
        }
        catch (Exception e) {
            log.error("Unexpected error during authentication for user {}: {}",
                    loginRequest.username(),
                    e.getMessage()
            );
            throw new IllegalStateException("Internal server error", e);
        }

        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        checkUserLocked();
        checkUserSuspend(user);

        log.info("User {}(id: {}) authenticated successfully. Generating refresh-token...", user.getUsername(), user.getId());

        String token = refreshTokenService.createRefreshToken(user, ip, userAgent, deviceId);

        return LoginResponse.builder()
                .token(token)
                .build();
    }

    private void checkUserSuspend(User user) {
        if (!user.isActive()) {
            log.warn("User {} is suspend", user.getUsername());
            throw new UserSuspendException("User suspended", user.getSuspendReason());
        }
    }

    private void checkUserLocked() {

    }
}
