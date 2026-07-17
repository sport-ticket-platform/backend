package com.backend.config.schedules;

import com.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    // 5:00 AM
    @Scheduled(cron = "0 0 5 * * *")
    public void cleanExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens...");

        int deletedCount = refreshTokenRepository.revokeAllExpiredTokens(LocalDateTime.now());

        log.info("Token cleaner finished | Deleted {} expired refresh tokens.", deletedCount);
    }
}