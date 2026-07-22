package com.backend.service.auth;

import com.backend.common.ApiMessage;
import com.backend.dto.auth.login.*;
import com.backend.dto.auth.signup.SignupRequest;
import com.backend.dto.auth.signup.SignupResponse;
import com.backend.dto.user.UserDto;
import com.backend.handler.AuthException;
import com.backend.handler.CustomLockedException;
import com.backend.handler.UserSuspendException;
import com.backend.security.userdetails.CustomUserDetails;
import com.backend.grpc.*;

import com.backend.service.system.RateLimitService;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;
    private final TwoFactorService twoFactorSer;
    private final UserDetailsService userDetailsService;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public LoginResponse loginWithPassword(LoginWithPassRequest loginWithPassRequest, String ip, String userAgent, String deviceId) {
        String identifier = loginWithPassRequest.identifier();

        // Phone Normalizing
        if (identifier != null && identifier.matches("^(?:\\+98|0098|0)?9\\d{9}$")) {
            identifier = "0" + identifier.replaceFirst("^(?:\\+98|0098|0)?", "");
        }

        log.info("Attempting to authenticate user: {}", identifier);

        // lock checks sooner than other checks
        // because if a user locked and password is incorrect,
        // user can still brute force password until it's correct
        checkUserLocked(identifier);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            identifier,
                            loginWithPassRequest.password()
                    )
            );
        }
        catch (BadCredentialsException | CredentialsExpiredException e) {
            log.warn("Authentication failed for identifier {}: {}", identifier, e.getMessage());

            // increase failed attempt for this identifier
            rateLimitService.incrementFailedAttempts(identifier);

            throw e;
        }
        catch (AuthenticationServiceException e) {
            log.error("Infrastructure error during auth for {}: {}", identifier, e.getMessage());
            throw e;
        }
        catch (AuthenticationException e) {
            log.error("Authentication error for identifier {}: {}", identifier, e.getMessage());
            throw new IllegalStateException("Authentication error occurred", e);
        }
        catch (Exception e) {
            log.error("Unexpected error during authentication for identifier {}: {}", identifier, e.getMessage());
            throw new IllegalStateException("Internal server error", e);
        }

        UserDto user = ((CustomUserDetails) Objects.requireNonNull(authentication.getPrincipal())).getUser();

        checkUserSuspend(user);

        // checking for 2fa
        if (user.isTwoFactorEnabled()) {
            log.info("User with id: {} authenticated successfully. 2FA is on...", user.getId());
            // Default is send notif with email
            String mfa = twoFactorSer.initiate2FA(user.getId(), user.getEmail(), true);
            return LoginResponse.builder()
                    .step("2FA-EMAIL")
                    .mfa_token(mfa)
                    .build();
        }

        log.info("User with id: {} authenticated successfully. Generating refresh-token...", user.getId());

        String token = refreshTokenService.create(user.getId(), ip, userAgent, deviceId);

        // login is completely successful in both state(2fa on or off)
        // (if it's on it will delete after correct otp) so deleting all previous failed attempts
        rateLimitService.clearFailedAttempts(identifier);

        return LoginResponse.builder()
                .step("SUCCESS")
                .refresh_token(token)
                .build();
    }

    public LoginResponse loginWithOTPEmail(LoginOTPEmailRequest otpEmailRequest) {
        return processOtpLogin(otpEmailRequest.email(), "2FA-EMAIL", "email");
    }

    public LoginResponse loginWithOTPPhone(LoginOTPPhoneRequest otpPhoneRequest) {

        String rawPhone = "0" + otpPhoneRequest.phone().replaceFirst("^(?:\\+98|0098|0)?", "");

        return processOtpLogin(rawPhone, "2FA-PHONE", "phone");
    }

    private LoginResponse processOtpLogin(String identifier, String step, String type) {
        log.info("Attempting OTP({}) login initiation for user: {}", type, identifier);

        // check user mfa locked
        checkUserMFALocked(identifier);

        // ==============================================================================

        boolean shouldSilentDrop = false;
        String silentDropReason = "";
        UserDto user = null;

        // user exist?
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(identifier);
            user = ((CustomUserDetails) userDetails).getUser();
        } catch (UsernameNotFoundException e) {
            shouldSilentDrop = true;
            silentDropReason = "User not found";
        }

        // user locked?
        if (!shouldSilentDrop) {
            Long lockExpirationTime = rateLimitService.getLockExpirationTime(identifier);
            if (lockExpirationTime != null && lockExpirationTime > System.currentTimeMillis()) {
                shouldSilentDrop = true;
                silentDropReason = "User is temporarily locked due to failed attempts";
            }
        }

        // user Suspend?
        if (!shouldSilentDrop && !user.isActive()) {
            shouldSilentDrop = true;
            silentDropReason = "User account is suspended";
        }

        String mfaToken;

        if (shouldSilentDrop) {
            log.warn("OTP login silent drop for identifier [{}]. Reason: {}", identifier, silentDropReason);

            // fake token
            mfaToken = java.util.UUID.randomUUID().toString().replace("-", "") + java.util.UUID.randomUUID().toString().substring(0, 10);

            // apply mfa lock
            twoFactorSer.applyMfaCooldown(identifier);

        } else {
            // everything is fine
            log.info("User {} is valid. Initiating real OTP flow...", user.getId());
            mfaToken = twoFactorSer.initiate2FA(user.getId(), identifier, true);
        }

        return LoginResponse.builder()
                .step(step)
                .mfa_token(mfaToken)
                .build();
    }

    private void checkUserMFALocked(String identifier) {
        long cooldownSeconds = twoFactorSer.getMfaCooldown(identifier);
        if (cooldownSeconds > 0) {
            long hours = cooldownSeconds / 3600;
            long minutes = (cooldownSeconds % 3600) / 60;
            long seconds = cooldownSeconds % 60;

            log.warn("Identifier: {} is on cooldown. Rejecting request.", identifier);
            throw new CustomLockedException(
                    ApiMessage.LOGIN_MFA_COOLDOWN,
                    "request MFA token too early",
                    seconds, minutes, hours
            );
        }
    }

    public LoginResponse verifyOTP(VerifyRequest verifyRequest, String ip, String userAgent, String deviceId) {

        Long userId = twoFactorSer.verify2FA(verifyRequest.mfa(), verifyRequest.otp());

        log.info("OTP verified successfully for user ID: {}. Generating refresh-token...", userId);
        String token = refreshTokenService.create(userId, ip, userAgent, deviceId);
        return LoginResponse.builder()
                .step("SUCCESS")
                .refresh_token(token)
                .build();
    }

    private void checkUserSuspend(UserDto user) {
        if (!user.isActive()) {
            log.warn("User with id {} is suspend", user.getId());
            throw new UserSuspendException("User suspended", "Account is deactivated.");
        }
    }

    private void checkUserLocked(String identifier) {
        Long lockExpirationTime = rateLimitService.getLockExpirationTime(identifier);

        if (lockExpirationTime == null) {
            return; // account not locked
        }

        long now = System.currentTimeMillis();

        if (lockExpirationTime < now) {
            return; // account not locked
        }

        long remainingMs = lockExpirationTime - now;
        long totalSeconds = remainingMs / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        log.warn("Authenticate failed for user: {} | account is locked for {} hours, {} minutes and {} seconds", identifier,  hours, minutes, seconds);

        throw new CustomLockedException(
                ApiMessage.ACCOUNT_LOCKED,
                "Account is temporarily locked.",
                seconds,
                minutes,
                hours
        );
    }

    private void checkEmailUnique(String email) {
        EmailExistsResponse response = userServiceStub.checkEmailExists(
                CheckEmailExistsRequest.newBuilder().setEmail(email).build()
        );
        if (response.getExists()) {
            log.warn("Signup failed. Email [{}] is already registered.", email);
            throw new AuthException(ApiMessage.SIGNUP_EMAIL_TAKEN);
        }
    }

    public SignupResponse signup(SignupRequest request) {
        String email = request.email() != null ? request.email().toLowerCase().trim() : "";
        String phone = request.phone() != null ? request.phone().trim() : "";

        checkEmailUnique(email);

        try {
            CreateUserRequest grpcRequest = CreateUserRequest.newBuilder()
                    .setEmail(email)
                    .setFirstName(request.first_name().trim())
                    .setLastName(request.last_name().trim())
                    .setPassword(passwordEncoder.encode(request.password().trim()))
                    .build();

            CreateUserResponse response = userServiceStub.createUser(grpcRequest);
            log.info("User successfully registered via gRPC. User ID: {}", response.getUserId());

            return SignupResponse.builder()
                    .user_id(response.getUserId())
                    .build();

        } catch (StatusRuntimeException e) {
            log.warn("gRPC error during signup. Status: {}", e.getStatus().getCode(), e);

            throw new AuthException(ApiMessage.SIGNUP_INTERNAL_ERROR, "Registration failed on server.");
        }
    }
}