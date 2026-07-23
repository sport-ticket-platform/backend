package com.backend.service.auth;

import com.backend.common.ApiMessage;
import com.backend.dto.auth.VerifyRequest;
import com.backend.dto.auth.login.*;
import com.backend.dto.auth.reset_password.ResetPasswordCompleteRequest;
import com.backend.dto.auth.reset_password.ResetPasswordInitiateRequest;
import com.backend.dto.auth.reset_password.ResetPasswordInitiateResponse;
import com.backend.dto.auth.reset_password.ResetPasswordVerifyResponse;
import com.backend.dto.user.UserDto;
import com.backend.grpc.ResetPasswordRequest;
import com.backend.grpc.ResetPasswordResponse;
import com.backend.grpc.UserServiceGrpc;
import com.backend.handler.AuthException;
import com.backend.handler.CustomLockedException;
import com.backend.handler.UserSuspendException;
import com.backend.security.userdetails.CustomUserDetails;
import com.backend.service.system.RateLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private static final String MFA_PURPOSE_LOGIN = "login";
    private static final String MFA_PURPOSE_RESET_PASSWORD = "reset_password";

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;
    private final TwoFactorService twoFactorSer;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;


    public LoginResponse loginWithPassword(LoginWithPassRequest loginWithPassRequest, String ip, String userAgent, String deviceId) {
        String identifier = loginWithPassRequest.identifier();

        if (identifier != null && identifier.matches("^(?:\\+98|0098|0)?9\\d{9}$")) {
            identifier = "0" + identifier.replaceFirst("^(?:\\+98|0098|0)?", "");
        }

        log.info("Attempting to authenticate user: {}", identifier);

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

        if (user.isTwoFactorEnabled()) {
            log.info("User with id: {} authenticated successfully. 2FA is on...", user.getId());
            String mfa = twoFactorSer.initiate2FA(user.getId(), user.getEmail(), true, MFA_PURPOSE_LOGIN);
            log.info("Login MFA token successfully generated and sent for user id: {}",  user.getId());
            return LoginResponse.builder()
                    .step("2FA-EMAIL")
                    .mfa_token(mfa)
                    .build();
        }

        log.info("User with id: {} authenticated successfully. Generating refresh-token...", user.getId());

        String token = refreshTokenService.create(user.getId(), ip, userAgent, deviceId);

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

        checkUserMFALocked(identifier);

        boolean shouldSilentDrop = false;
        String silentDropReason = "";
        UserDto user = null;

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);
            user = ((CustomUserDetails) userDetails).getUser();
        } catch (UsernameNotFoundException e) {
            shouldSilentDrop = true;
            silentDropReason = "User not found";
        }

        if (!shouldSilentDrop) {
            Long lockExpirationTime = rateLimitService.getLockExpirationTime(identifier);
            if (lockExpirationTime != null && lockExpirationTime > System.currentTimeMillis()) {
                shouldSilentDrop = true;
                silentDropReason = "User is temporarily locked due to failed attempts";
            }
        }

        if (!shouldSilentDrop && !user.isActive()) {
            shouldSilentDrop = true;
            silentDropReason = "User account is suspended";
        }

        String mfaToken;

        if (shouldSilentDrop) {
            log.warn("OTP login silent drop for identifier [{}]. Reason: {}", identifier, silentDropReason);
            mfaToken = java.util.UUID.randomUUID().toString().replace("-", "") + java.util.UUID.randomUUID().toString().substring(0, 10);
            twoFactorSer.applyMfaCooldown(identifier);
        } else {
            log.info("User {} is valid. Initiating real OTP flow...", user.getId());
            mfaToken = twoFactorSer.initiate2FA(user.getId(), identifier, true, MFA_PURPOSE_LOGIN);
        }

        return LoginResponse.builder()
                .step(step)
                .mfa_token(mfaToken)
                .build();
    }

    public LoginResponse verifyLoginOTP(VerifyRequest verifyRequest, String ip, String userAgent, String deviceId) {

        // ارسال Purpose به متد اعتبارسنجی
        TwoFactorService.MfaVerificationResult result = twoFactorSer.verify2FA(verifyRequest.mfa(), verifyRequest.otp(), MFA_PURPOSE_LOGIN);

        Long userId = result.userId();
        log.info("user id: {}", userId);

        if (userId == null) {
            log.warn("Attempt to login with a signup MFA token. Identifier: {}", result.identifier());
            throw new AuthException(ApiMessage.LOGIN_OTP_WRONG);
        }

        log.info("OTP verified successfully for user ID: {}. Generating refresh-token...", userId);
        String token = refreshTokenService.create(userId, ip, userAgent, deviceId);

        return LoginResponse.builder()
                .step("SUCCESS")
                .refresh_token(token)
                .build();
    }

    // ==============================================================================
    // RESET PASSWORD
    // ==============================================================================

    public ResetPasswordInitiateResponse initiatePasswordReset(ResetPasswordInitiateRequest request) {
        String email = request.email();

        log.info("Attempting password reset initiation for user: {}", email);

        checkUserMFALocked(email);

        boolean shouldSilentDrop = false;
        String silentDropReason = "";
        UserDto user = null;

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            user = ((CustomUserDetails) userDetails).getUser();
        } catch (UsernameNotFoundException e) {
            shouldSilentDrop = true;
            silentDropReason = "User not found";
        }

        if (!shouldSilentDrop && !user.isActive()) {
            shouldSilentDrop = true;
            silentDropReason = "User account is suspended";
        }

        String mfaToken;

        if (shouldSilentDrop) {
            log.warn("Password reset silent drop for identifier [{}]. Reason: {}", email, silentDropReason);
            mfaToken = java.util.UUID.randomUUID().toString().replace("-", "") + java.util.UUID.randomUUID().toString().substring(0, 10);
            twoFactorSer.applyMfaCooldown(email);
        } else {
            log.info("User {} with email[{}] is valid. Initiating real OTP flow for password reset...", user.getId(), email);
            mfaToken = twoFactorSer.initiate2FA(user.getId(), email, true, MFA_PURPOSE_RESET_PASSWORD);
        }

        return ResetPasswordInitiateResponse.builder()
                .mfa_token(mfaToken)
                .build();
    }

    public ResetPasswordVerifyResponse verifyResetPasswordOTP(VerifyRequest request) {

        // ارسال Purpose به متد اعتبارسنجی
        TwoFactorService.MfaVerificationResult result = twoFactorSer.verify2FA(request.mfa(), request.otp(), MFA_PURPOSE_RESET_PASSWORD);

        Long userId = result.userId();

        if (userId == null) {
            log.warn("Attempt to reset password with a signup MFA token. Identifier: {}", result.identifier());
            throw new AuthException(ApiMessage.LOGIN_OTP_WRONG);
        }

        log.info("OTP verified successfully for password reset. User ID: {}. Generating temp token...", userId);

        String tempToken = java.util.UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set(
                "reset:temp:" + tempToken,
                String.valueOf(userId),
                15,
                java.util.concurrent.TimeUnit.MINUTES
        );

        log.info("Temporary reset token successfully generated and cached for user ID: {}", userId);

        return ResetPasswordVerifyResponse.builder()
                .temp_token(tempToken)
                .build();
    }

    public void completePasswordReset(ResetPasswordCompleteRequest request) {
        String tempTokenKey = "reset:temp:" + request.temp_token();
        String userIdStr = redisTemplate.opsForValue().get(tempTokenKey);

        if (userIdStr == null) {
            log.warn("Password reset completion failed. Temp token expired or invalid: [{}]", request.temp_token());
            throw new AuthException(ApiMessage.SIGNUP_INVALID_TEMP_TOKEN);
        }

        long userId = Long.parseLong(userIdStr);
        log.info("Completing password reset for user ID: [{}]", userId);

        String hashedPassword = passwordEncoder.encode(request.password());

        try {
            ResetPasswordRequest grpcRequest = ResetPasswordRequest.newBuilder()
                    .setId(userId)
                    .setNewPassword(hashedPassword)
                    .build();

            ResetPasswordResponse userServiceResponse = userServiceStub.changeUserPassword(grpcRequest);

            if (!userServiceResponse.getSuccess()) {
                log.warn("Password reset failed in user-service for user ID: [{}]", userId);
                throw new AuthException(ApiMessage.RESET_PASSWORD_FAILED);
            }

        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC error while resetting password for user ID [{}]: {}", userId, e.getStatus());
            throw e;
        }

        redisTemplate.delete(tempTokenKey);

        log.info("Password successfully reset for user ID: [{}]", userId);
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

    private void checkUserSuspend(UserDto user) {
        if (!user.isActive()) {
            log.warn("User with id {} is suspend", user.getId());
            throw new UserSuspendException("User suspended", "Account is deactivated.");
        }
    }

    private void checkUserLocked(String identifier) {
        Long lockExpirationTime = rateLimitService.getLockExpirationTime(identifier);

        if (lockExpirationTime == null) {
            return;
        }

        long now = System.currentTimeMillis();

        if (lockExpirationTime < now) {
            return;
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
}