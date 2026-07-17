package com.backend.repository;

import com.backend.model.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RefreshToken> mapper = (rs, rowNum) -> RefreshToken.builder()
            .id(rs.getLong("token_id"))
            .token(rs.getString("token"))
            .userId(rs.getLong("user_id"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .expirationDate(rs.getTimestamp("expiration_date") != null ? rs.getTimestamp("expiration_date").toLocalDateTime() : null)
            .isActive(rs.getBoolean("is_active"))
            .revokedAt(rs.getTimestamp("revoked_at") != null ? rs.getTimestamp("revoked_at").toLocalDateTime() : null)
            .revokedReason(rs.getString("revoked_reason"))
            .ipAddress(rs.getString("ip_address"))
            .userAgent(rs.getString("user_agent"))
            .deviceId(rs.getString("device_id"))
            .build();

    public RefreshToken save(RefreshToken token) {
        String sql = """
            INSERT INTO refresh_token 
            (token, user_id, created_at, expiration_date, is_active, ip_address, user_agent, device_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime created = token.getCreatedAt() != null ? token.getCreatedAt() : LocalDateTime.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, token.getToken());
            ps.setLong(2, token.getUserId());
            ps.setTimestamp(3, Timestamp.valueOf(created));
            ps.setTimestamp(4, Timestamp.valueOf(token.getExpirationDate()));
            ps.setBoolean(5, token.isActive());
            ps.setString(6, token.getIpAddress());
            ps.setString(7, token.getUserAgent());
            ps.setString(8, token.getDeviceId());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("token_id")) {
            token.setId(((Number) keyHolder.getKeys().get("token_id")).longValue());
        }
        token.setCreatedAt(created);
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        String sql = "SELECT * FROM refresh_token WHERE token = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, mapper, token));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public int deactivateByUser(Long userId, LocalDateTime time, String reason) {
        String sql = """
            UPDATE refresh_token 
            SET is_active = false, revoked_at = ?, revoked_reason = ? 
            WHERE user_id = ? AND is_active = true
            """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(time), reason, userId);
    }

    public int revokeAllTokensForUser(Long userId, String banReason) {
        String sql = """
            UPDATE refresh_token 
            SET is_active = false, revoked_at = ?, revoked_reason = ? 
            WHERE user_id = ? AND is_active = true
            """;

        return jdbcTemplate.update(
                sql,
                Timestamp.valueOf(LocalDateTime.now()),
                banReason,
                userId
        );
    }

    public int revokeToken(String token, String banReason) {
        String sql = """
            UPDATE refresh_token 
            SET is_active = false, revoked_at = ?, revoked_reason = ? 
            WHERE token = ? AND is_active = true
            """;

        return jdbcTemplate.update(
                sql,
                Timestamp.valueOf(LocalDateTime.now()),
                banReason,
                token
        );
    }

    public int revokeAllExpiredTokens(LocalDateTime time) {
        String sql = """
            UPDATE refresh_token 
            SET is_active = false, revoked_at = ?, revoked_reason = ? 
            WHERE expiration_date < ? AND is_active = true
            """;

        return jdbcTemplate.update(
                sql,
                Timestamp.valueOf(time),
                "System Scheduled Cleanup | Expired Token",
                Timestamp.valueOf(time)
        );
    }
}