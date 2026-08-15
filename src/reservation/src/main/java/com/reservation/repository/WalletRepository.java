package com.reservation.repository;

import com.reservation.dto.PageResult;
import com.reservation.model.wallet.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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

    /**
     * Finds the wallet by User ID
     */
    public Optional<Wallet> findByUserId(Long userId) {
        String sql = "SELECT wallet_id, user_id, balance, is_active FROM wallet WHERE user_id = :user_id";

        try {
            Wallet wallet = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("user_id", userId),
                    (rs, rowNum) -> Wallet.builder()
                            .walletId(rs.getLong("wallet_id"))
                            .userId(rs.getLong("user_id"))
                            .balance(rs.getBigDecimal("balance"))
                            .isActive(rs.getBoolean("is_active"))
                            .build()
            );
            return Optional.ofNullable(wallet);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Fetches paginated wallet transactions with optional filters.
     */
    public PageResult<WalletTransaction> getWalletTransactions(
            Long walletId,
            TransactionType type,
            TransactionStatus status,
            int page,
            int pageSize) {

        int limit = pageSize;
        int offset = page * pageSize;

        // کوئری شمارش کل رکوردها (برای صفحه‌بندی) با کست کردن ENUMهای دیتابیس
        String countSql = """
            SELECT COUNT(*) FROM wallet_transaction
            WHERE wallet_id = :wallet_id
              AND (:type IS NULL OR type = CAST(:type AS transaction_type))
              AND (:status IS NULL OR status = CAST(:status AS transaction_status))
        """;

        // کوئری واکشی اطلاعات
        String fetchSql = """
            SELECT transaction_id, wallet_id, type, status, amount, balance_after, description,
                   reference_type, reference_id, created_at, updated_at
            FROM wallet_transaction
            WHERE wallet_id = :wallet_id
              AND (:type IS NULL OR type = CAST(:type AS transaction_type))
              AND (:status IS NULL OR status = CAST(:status AS transaction_status))
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
        """;

        // از null بودن فیلترها پشتیبانی می‌کنیم
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("wallet_id", walletId)
                .addValue("type", type != null ? type.name() : null)
                .addValue("status", status != null ? status.name() : null)
                .addValue("limit", limit)
                .addValue("offset", offset);

        Integer totalElements = jdbcTemplate.queryForObject(countSql, params, Integer.class);
        totalElements = totalElements != null ? totalElements : 0;

        List<WalletTransaction> transactions = jdbcTemplate.query(fetchSql, params, (rs, rowNum) -> {
            // چون reference_type در دیتابیس می‌تواند null باشد (برای شارژهای دستی و ...)
            String refTypeStr = rs.getString("reference_type");
            TransactionReferenceType refType = refTypeStr != null ? TransactionReferenceType.valueOf(refTypeStr) : null;
            Long refId = rs.getObject("reference_id") != null ? rs.getLong("reference_id") : null;

            return WalletTransaction.builder()
                    .transactionId(rs.getLong("transaction_id"))
                    .walletId(rs.getLong("wallet_id"))
                    .type(TransactionType.valueOf(rs.getString("type")))
                    .status(TransactionStatus.valueOf(rs.getString("status")))
                    .amount(rs.getBigDecimal("amount"))
                    .balanceAfter(rs.getBigDecimal("balance_after"))
                    .description(rs.getString("description"))
                    .referenceType(refType)
                    .referenceId(refId)
                    .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                    .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                    .build();
        });

        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        return new PageResult<>(
                transactions,
                totalElements,
                totalPages,
                page,
                pageSize,
                page < totalPages - 1, // has_next
                page > 0               // has_previous
        );
    }
}