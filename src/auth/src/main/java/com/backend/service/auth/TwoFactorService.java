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

        redisTemplate.opsForValue().set(
                redisKey,
                userId.toString() + ":" + identifier + ":" + otpCode,
                appPrp.getMfaTokenTtlMin(),
                TimeUnit.MINUTES
        );

        applyMfaCooldown(identifier);

        // send otp
        sendOtpToUser(identifier, otpCode, isEmail);

        log.info("MFA initiated for user ID: {}. Token generated.", userId);
        return mfaToken;
    }

    /**
     * <p>Validate MFA token and OTP code, return user ID if successful</p>
     */
    public Long verify2FA(String mfaToken, String enteredCode) {
        String redisKey = REDIS_MFA_PREFIX + mfaToken;
        String value = redisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            log.warn("MFA verification failed: Token {} expired or not found. Returning generic OTP wrong error.", mfaToken);
            throw new AuthException(ApiMessage.LOGIN_OTP_WRONG, "otp");
        }

        String[] parts = value.split(":");
        if (parts.length != 3) {
            log.error("Corrupted MFA data in Redis for token: {}", mfaToken);
            throw new IllegalStateException("Internal server error: Invalid data format in cache.");
        }

        Long userId = Long.parseLong(parts[0]);
        String identifier = parts[1]; // for rate limit serv & cooldown
        String savedOtpCode = parts[2];

        String cooldownKey = REDIS_MFA_COOLDOWN_PREFIX + identifier;

        redisTemplate.delete(redisKey);
        redisTemplate.delete(cooldownKey);

        if (!savedOtpCode.equals(enteredCode.trim())) {
            log.warn("MFA verification failed for user ID: {}. Incorrect OTP code.", userId);
            rateLimitService.incrementFailedAttempts(identifier);
            throw new AuthException(ApiMessage.LOGIN_OTP_WRONG, "otp");
        }

        rateLimitService.clearFailedAttempts(identifier);
        log.info("MFA successfully verified for user ID: {}", userId);

        return userId;
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