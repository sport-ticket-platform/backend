package com.backend.service.auth;

import com.backend.common.ApiMessage;
import com.backend.config.ApplicationProperties;
import com.backend.handler.AuthException;
import com.backend.handler.CustomLockedException;
import com.backend.service.system.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorService {

    private final StringRedisTemplate redisTemplate;
    private final ApplicationProperties appPrp;
    private final RateLimitService rateLimitService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
    private final String REDIS_MFA_PREFIX = "mfa:token:";
    private final String REDIS_MFA_COOLDOWN_PREFIX = "mfa:cooldown:";
    private static final String GUEST_USER_MARKER = "GUEST"; // for signup

    public record MfaVerificationResult(Long userId, String identifier) {}

    public long getMfaCooldown(String identifier) {
        String cooldownKey = REDIS_MFA_COOLDOWN_PREFIX + identifier;
        Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
        return ttl > 0 ? ttl : 0;
    }

    public void applyMfaCooldown(String identifier) {
        String cooldownKey = REDIS_MFA_COOLDOWN_PREFIX + identifier;
        redisTemplate.opsForValue().set(
                cooldownKey,
                "LOCKED",
                appPrp.getMfaTokenTtlMin(),
                TimeUnit.MINUTES
        );
    }

    /**
     * <p>create mfa token in redis, then call notif service</p>
     * userId can be null for new user signups.
     */
    public String initiate2FA(Long userId, String identifier, boolean isEmail) {

        long remainingSeconds = getMfaCooldown(identifier);

        if (remainingSeconds > 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;

            log.warn("Identifier: {} requested new MFA token too early. next mfa in {}h, {}m, {}s", identifier, hours, minutes, seconds);

            throw new CustomLockedException(
                    ApiMessage.LOGIN_MFA_COOLDOWN,
                    "request MFA token too early",
                    seconds, minutes, hours
            );
        }

        String mfaToken = generateSecureToken();
        String otpCode = generateOtpCode();
        String redisKey = REDIS_MFA_PREFIX + mfaToken;

        // handle userId == null for signup
        String userIdStr = (userId != null) ? userId.toString() : GUEST_USER_MARKER;

        redisTemplate.opsForValue().set(
                redisKey,
                userIdStr + ":" + identifier + ":" + otpCode,
                appPrp.getMfaTokenTtlMin(),
                TimeUnit.MINUTES
        );

        applyMfaCooldown(identifier);

        // send otp
        sendOtpToUser(identifier, otpCode, isEmail);

        log.info("MFA initiated for identifier: {}. Token generated.", identifier);
        return mfaToken;
    }

    /**
     * <p>Validate MFA token and OTP code, return Result object</p>
     */
    public MfaVerificationResult verify2FA(String mfaToken, String enteredCode) {
        String redisKey = REDIS_MFA_PREFIX + mfaToken;
        String value = redisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            log.warn("MFA verification failed: Token {} expired or not found.", mfaToken);
            throw new AuthException(ApiMessage.LOGIN_OTP_WRONG, "otp");
        }

        String[] parts = value.split(":");
        if (parts.length != 3) {
            log.error("Corrupted MFA data in Redis for token: {}", mfaToken);
            throw new IllegalStateException("Internal server error: Invalid data format in cache.");
        }

        String userIdStr = parts[0];
        String identifier = parts[1]; // for rate limit serv & cooldown
        String savedOtpCode = parts[2];

        Long userId = GUEST_USER_MARKER.equals(userIdStr) ? null : Long.parseLong(userIdStr);

        String cooldownKey = REDIS_MFA_COOLDOWN_PREFIX + identifier;

        redisTemplate.delete(redisKey);
        redisTemplate.delete(cooldownKey);

        if (!savedOtpCode.equals(enteredCode.trim())) {
            log.warn("MFA verification failed for identifier: {}. Incorrect OTP code.", identifier);

            rateLimitService.incrementFailedAttempts(identifier);

            throw new AuthException(ApiMessage.LOGIN_OTP_WRONG, "otp");
        }

        rateLimitService.clearFailedAttempts(identifier);

        log.info("MFA successfully verified for identifier: {}", identifier);

        return new MfaVerificationResult(userId, identifier);
    }

    public String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    private String generateOtpCode() {
        int number = secureRandom.nextInt(90_000) + 10_000;
        return String.valueOf(number);
    }

    private void sendOtpToUser(String email, String code, boolean isEmail) {
        // TODO: call the notif service
    }
}