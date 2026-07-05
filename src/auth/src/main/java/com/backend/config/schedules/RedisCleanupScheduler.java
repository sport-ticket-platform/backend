package com.backend.config.schedules;

import com.backend.service.system.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RedisCleanupScheduler {

    private final RateLimitService rateLimitService;
    // 6 Hours
    @Scheduled(fixedRate = 21_600_000)
    public void cleanExpiredLocks() {
        long count =rateLimitService.getTotalLockedUsersCount();
        log.info("RedisCleanupScheduler.cleanExpiredLocks cleaned expired locked users | current locked users after cleanup: {}", count);
    }
}