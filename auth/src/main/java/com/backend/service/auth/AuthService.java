package com.backend.service.auth;

import com.backend.common.ApiMessage;
import com.backend.dto.auth.*;
import com.backend.entity.User;
import com.backend.entity.UserRole;
import com.backend.handler.AuthException;
import com.backend.handler.UserSuspendException;
import com.backend.repository.UserRepository;
import com.backend.security.userdetails.CustomUserDetails;
import com.backend.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

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

    // Signup
    public CheckUsernameResponse checkUsernameUnique(String username) {

        boolean isUnique = !userRepository.existsByUsername(username);

        log.debug("Username '{}' uniqueness result: {}", username, isUnique);

        return CheckUsernameResponse.builder()
                .is_unique(isUnique)
                .build();
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {

        if (!checkUsernameUnique(request.username()).is_unique()) {
            log.warn("Signup failed. Username [{}] is already taken.", request.username());
            throw new AuthException(ApiMessage.SIGNUP_USERNAME_TAKEN, "username");
        }

        try {
            User user = User.builder()
                    .username(request.username().toLowerCase().trim())
                    .firstName(request.first_name().trim())
                    .lastName(request.last_name().trim())
                    .password(passwordEncoder.encode(request.password().trim()))
                    .role(UserRole.USER)
                    .isActive(true)
                    .numberVerified(false)
                    .emailVerified(false)
                    .isCredentialExpired(false)
                    .build();

            User savedUser = userRepository.save(user);
            log.info("User [{}] successfully registered.", savedUser.getUsername());

            return SignupResponse.builder()
                    .userId(savedUser.getId())
                    .build();

        } catch (DataIntegrityViolationException e) {
            log.warn("Database constraint violation for user [{}]", request.username(), e);
            throw new AuthException(ApiMessage.SIGNUP_DATABASE_ERROR, "username");
        }
    }
}
