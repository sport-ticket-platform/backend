package com.backend.service.system;

import com.backend.config.ApplicationPolicies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final ApplicationPolicies appPolicies;

    public static final String FAILED_LOGIN = "auth:failed_attempts:";
    public static final String LOCKED_USERS_ZSET = "auth:locked_users_zset";


    public boolean isIpAllowed(String ip, String endpoint, int maxRequests, long timeWindowSeconds) {
        String key = "rate_limit:" + endpoint + ":" + ip;

        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(timeWindowSeconds));
        }
        if (currentCount != null && currentCount > maxRequests) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", ip, endpoint);
            return false;
        }

        return true;
    }

    public void incrementFailedAttempts(String identifier) {
        String key = FAILED_LOGIN + identifier;

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, Duration.ofHours(6));
        }

        if (currentCount != null && currentCount >= appPolicies.getMaxFailedLoginAttempts()) {
            // after max attempts, every wrong password that user entered lockout is longer
            long multiplier = (currentCount - appPolicies.getMaxFailedLoginAttempts()) + 1;
            long lockTime = appPolicies.getAccountLockoutDurationSecond() * multiplier;
            lockUser(identifier, lockTime);
        }

        log.info("Failed login attempt registered for user: {}. Total failures: {}", identifier, currentCount);
    }

    public int getUserFailedAttempts(String identifier) {
        String key = FAILED_LOGIN + identifier;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    public void lockUser(String identifier, long lockoutDurationSeconds) {
        long expirationTimeMs = System.currentTimeMillis() + (lockoutDurationSeconds * 1000L);

        // adding to zset with score=expiration-time
        redisTemplate.opsForZSet().add(LOCKED_USERS_ZSET, identifier, expirationTimeMs);

        log.info("User [{}] locked until timestamp: {}", identifier, expirationTimeMs);
    }

    public void clearFailedAttempts(String identifier) {
        String key = FAILED_LOGIN + identifier;
        redisTemplate.delete(key);
    }

    public void unlockUser(String identifier) {
        redisTemplate.opsForZSet().remove(LOCKED_USERS_ZSET, identifier);
    }

    public Long getLockExpirationTime(String identifier) {
        Double expirationTimeMs = redisTemplate.opsForZSet().score(LOCKED_USERS_ZSET, identifier);

        if (expirationTimeMs == null) {
            return null;
        }

        if (expirationTimeMs < System.currentTimeMillis()) {
            redisTemplate.opsForZSet().remove(LOCKED_USERS_ZSET, identifier);
            return null;
        }

        return expirationTimeMs.longValue();
    }

    public long getTotalLockedUsersCount() {
        long now = System.currentTimeMillis();

        // deleting passed time records
        redisTemplate.opsForZSet().removeRangeByScore(LOCKED_USERS_ZSET, 0, now - 1);

        Long count = redisTemplate.opsForZSet().count(LOCKED_USERS_ZSET, now, Double.POSITIVE_INFINITY);
        return count != null ? count : 0;
    }

    /**
     * Returns a paginated result.
     *
     * <p>The returned records are in the range:
     * {@code (page - 1) * size} (inclusive) to
     * {@code page * size} (exclusive).
     *
     * @param page the page number (starting from 1)
     * @param size the number of records per page
     */
    public Set<String> getLockedUsersPaginated(int page, int size) {
        long now = System.currentTimeMillis();

        long offset = (page - 1L) * size;
        long total = getTotalLockedUsersCount();

        if (offset >= total) {
            return Collections.emptySet();
        }

        return redisTemplate.opsForZSet().rangeByScore(
                LOCKED_USERS_ZSET,
                now,
                Double.POSITIVE_INFINITY,
                offset,
                size
        );
    }
}