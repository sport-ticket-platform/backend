package com.backend.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WalletRepository {

    private final JdbcTemplate jdbcTemplate;

    public void createWallet(Long userId) {
        String sql = "INSERT INTO wallet (user_id) VALUES (?)";
        try {
            jdbcTemplate.update(sql, userId);
            log.info("Wallet created successfully for user_id: [{}]", userId);
        } catch (Exception e) {
            log.error("Failed to create wallet for user_id: [{}]", userId, e);
            throw new RuntimeException("Failed to initialize user wallet");
        }
    }
}