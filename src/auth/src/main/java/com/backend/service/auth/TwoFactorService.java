package com.backend.service.auth;

import com.backend.common.ApiMessage;
import com.backend.config.ApplicationProperties;
import com.backend.handler.AuthException;
import com.backend.handler.CustomLockedException;
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
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
    private final String REDIS_MFA_PREFIX = "mfa:token:";
    private final String REDIS_MFA_COOLDOWN_PREFIX = "mfa:cooldown:";


    /**
     * <p>create mfa token in redis, then call notif service</p>
     */
    public String initiate2FA(Long userId, String identifier, boolean isEmail) {

        String cooldownKey = REDIS_MFA_COOLDOWN_PREFIX + userId;
        Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
        // can't request otp a lot
        if (ttl != null && ttl > 0) {
            long hours = ttl / 3600;
            long minutes = (ttl % 3600) / 60;
            long seconds = ttl % 60;

            log.warn("User ID: {} requested new MFA token too early. next mfa in {} hours, {} minutes and {} seconds", userId,  hours, minutes, seconds);

            throw new CustomLockedException(
                    ApiMessage.LOGIN_MFA_COOLDOWN,
                    "request MFA token too early",
                    seconds,
                    minutes,
                    hours
            );
        }

        String mfaToken = generateSecureToken();
        String otpCode = generateOtpCode();
        String redisKey = REDIS_MFA_PREFIX + mfaToken;
        redisTemplate.opsForValue().set(
                redisKey,
                userId.toString() + ":" + otpCode,
                appPrp.getMfaTokenTtlMin(),
                TimeUnit.MINUTES
        );

        redisTemplate.opsForValue().set(
                cooldownKey,
                "LOCKED",
                appPrp.getMfaTokenTtlMin(),
                TimeUnit.MINUTES
        );

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

        // MFA token is not valid (wrong or expired)
        if (value == null) {
            log.warn("MFA verification failed: Token expired or not found.");
            throw new AuthException(ApiMessage.LOGIN_MFA_EXPIRED, "mfa");
        }

        // Save in redis like = 32:22144 means user id=32 & otp=22144
        // So we should split it
        String[] parts = value.split(":");
        if (parts.length != 2) {
            log.error("Corrupted MFA data in Redis for token: {}", mfaToken);
            throw new IllegalStateException("Internal server error: Invalid data format in cache.");
        }
        Long userId = Long.parseLong(parts[0]);
        String savedOtpCode = parts[1];

        String cooldownKey = REDIS_MFA_COOLDOWN_PREFIX + userId;

        redisTemplate.delete(redisKey);
        redisTemplate.delete(cooldownKey);

        if (!savedOtpCode.equals(enteredCode.trim())) {
            log.warn("MFA verification failed for user ID: {}. Incorrect OTP code.", userId);
            throw new AuthException(ApiMessage.LOGIN_OTP_WRONG, "otp");
        }

        log.info("MFA successfully verified for user ID: {}", userId);

        return userId;
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    private String generateOtpCode() {
        // generate between 10,000 - 99,999
        int number = secureRandom.nextInt(90_000) + 10_000;
        return String.valueOf(number);
    }

    private void sendOtpToUser(String email, String code, boolean isEmail) {
        // TODO: call the notif service
    }
}
