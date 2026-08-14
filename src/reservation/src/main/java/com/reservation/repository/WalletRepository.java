package com.reservation.repository;

import com.reservation.model.wallet.TransactionReferenceType;
import com.reservation.model.wallet.TransactionStatus;
import com.reservation.model.wallet.TransactionType;
import com.reservation.model.wallet.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository layer for executing SQL queries on wallet and wallet_transaction tables.
 */
@Repository
@RequiredArgsConstructor
public class WalletRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<Wallet> walletRowMapper = (rs, rowNum) -> Wallet.builder()
            .walletId(rs.getLong("wallet_id"))
            .userId(rs.getLong("user_id"))
            .balance(rs.getBigDecimal("balance"))
            .isActive(rs.getBoolean("is_active"))
            .build();


    /**
     * Fetches user's wallet with row-level pessimistic locking (SELECT FOR UPDATE).
     * Prevents race conditions during concurrent balance deductions or updates.
     *
     * @param userId The ID of the user
     * @return Optional containing the Wallet if exists
     */
    public Optional<Wallet> findByUserIdForUpdate(Long userId) {
        String sql = "SELECT * FROM wallet WHERE user_id = :user_id FOR UPDATE";
        try {
            Wallet wallet = jdbcTemplate.queryForObject(
                    sql,
                    new MapSqlParameterSource("user_id", userId),
                    walletRowMapper
            );
            return Optional.ofNullable(wallet);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates an initial active wallet with zero balance for a user.
     */
    public Wallet createWallet(Long userId) {
        String sql = """
                INSERT INTO wallet (user_id, balance, is_active)
                VALUES (:user_id, 0.00, true)
                RETURNING *
                """;
        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("user_id", userId),
                walletRowMapper
        );
    }

    public void updateBalance(Long walletId, BigDecimal newBalance) {
        String sql = "UPDATE wallet SET balance = :balance WHERE wallet_id = :wallet_id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("balance", newBalance)
                .addValue("wallet_id", walletId);
        jdbcTemplate.update(sql, params);
    }

    public Long insertTransaction(
            Long walletId,
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description,
            TransactionReferenceType refType,
            Long refId
    ) {
        String sql = """
                INSERT INTO wallet_transaction
                (wallet_id, type, status, amount, balance_after, description, reference_type, reference_id)
                VALUES
                (:wallet_id, :type::transaction_type, :status::transaction_status, :amount,
                 :balance_after, :description, :ref_type::transaction_reference_type, :ref_id)
                RETURNING transaction_id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("wallet_id", walletId)
                .addValue("type", type.name())
                .addValue("status", status.name())
                .addValue("amount", amount)
                .addValue("balance_after", balanceAfter)
                .addValue("description", description)
                .addValue("ref_type", refType != null ? refType.name() : null)
                .addValue("ref_id", refId);

        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }
}