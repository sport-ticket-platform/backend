package com.backend.service.auth;

import com.backend.common.ApiMessage;
import com.backend.dto.auth.VerifyRequest;
import com.backend.dto.auth.signup.SignupCompleteRequest;
import com.backend.dto.auth.signup.SignupInitiateRequest;
import com.backend.dto.auth.signup.SignupInitiateResponse;
import com.backend.dto.auth.signup.SignupVerifyResponse;
import com.backend.grpc.*;
import com.backend.handler.AuthException;
import com.backend.handler.CustomLockedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignupService {

    private final TwoFactorService twoFactorSer;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public SignupInitiateResponse signupInitiate(SignupInitiateRequest request) {
        String email = request.email().toLowerCase().trim();

        checkEmailUnique(email);

        checkUserMFALocked(email);

        log.info("Initiating signup OTP for new email: {}", email);

        String mfaToken = twoFactorSer.initiate2FA(null, email, true);

        log.info("Signup MFA token successfully generated and sent for email: {}", email);

        return SignupInitiateResponse.builder()
                .mfa_token(mfaToken)
                .build();
    }

    public SignupVerifyResponse verifySignupOTP(VerifyRequest request) {

        TwoFactorService.MfaVerificationResult result = twoFactorSer.verify2FA(request.mfa(), request.otp());

        // prevent using login mfa token for signup
        if (result.userId() != null) {
            log.warn("Attempt to signup with a login MFA token. UserID: {}", result.userId());
            throw new AuthException(ApiMessage.LOGIN_OTP_WRONG);
        }

        String email = result.identifier();
        log.info("Signup OTP verified successfully for email: {}. Generating temp token...", email);

        String tempToken = java.util.UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set(
                "signup:temp:" + tempToken,
                email,
                15,
                java.util.concurrent.TimeUnit.MINUTES
        );

        log.info("Temporary signup token successfully generated and cached for email: {}", email);

        return SignupVerifyResponse.builder()
                .temp_token(tempToken)
                .build();
    }

    public void signupComplete(SignupCompleteRequest request) {
        String tempTokenKey = "signup:temp:" + request.temp_token();

        String email = redisTemplate.opsForValue().get(tempTokenKey);

        if (email == null) {
            log.warn("Signup completion failed. Temp token expired or invalid: [{}]", request.temp_token());
            throw new AuthException(ApiMessage.SIGNUP_INVALID_TEMP_TOKEN);
        }

        log.info("Completing registration for email: [{}]", email);

        String hashedPassword = passwordEncoder.encode(request.password());

        try {
            CreateUserRequest createUserReq = CreateUserRequest.newBuilder()
                    .setEmail(email)
                    .setFirstName(request.first_name())
                    .setLastName(request.last_name())
                    .setPassword(hashedPassword)
                    .build();

            CreateUserResponse userServiceResponse = userServiceStub.createUser(createUserReq);

            if (!userServiceResponse.getSuccess()) {
                log.warn("User creation failed in user-service for email: [{}]", email);
                throw new AuthException(ApiMessage.SIGNUP_FAILED);
            }

        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC error while creating user for email [{}]: {}", email, e.getStatus());
            throw new AuthException(ApiMessage.SIGNUP_FAILED);
        }

        // single use (delete only after successful user creation)
        redisTemplate.delete(tempTokenKey);

        log.info("User successfully registered for email: [{}]", email);
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

    private void checkEmailUnique(String email) {
        EmailExistsResponse response = userServiceStub.checkEmailExists(
                CheckEmailExistsRequest.newBuilder().setEmail(email).build()
        );
        if (response.getExists()) {
            log.warn("Signup failed. Email [{}] is already registered.", email);
            throw new AuthException(ApiMessage.SIGNUP_EMAIL_TAKEN);
        }
    }
}