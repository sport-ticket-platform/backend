package com.backend.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

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
}