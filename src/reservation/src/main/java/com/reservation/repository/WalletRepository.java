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
import java.util.Collections;
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
            int pageSize
    ) {

        String typeCondition = (type != null) ? " AND type = :type::transaction_type " : "";
        String statusCondition = (status != null) ? " AND status = :status::transaction_status " : "";

        String countSql = """
            SELECT COUNT(*)
            FROM wallet_transaction
            WHERE wallet_id = :wallet_id
            """ + typeCondition + statusCondition;

        String dataSql = """
            SELECT transaction_id, wallet_id, type, status, amount, balance_after, description,
                   reference_type, reference_id, created_at, updated_at
            FROM wallet_transaction
            WHERE wallet_id = :wallet_id
            """ + typeCondition + statusCondition + """
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("wallet_id", walletId)
                .addValue("limit", pageSize)
                .addValue("offset", page * pageSize);

        if (type != null) {
            params.addValue("type", type.name());
        }

        if (status != null) {
            params.addValue("status", status.name());
        }

        Long totalElements = jdbcTemplate.queryForObject(countSql, params, Long.class);

        List<WalletTransaction> content = Collections.emptyList();
        if (totalElements != null && totalElements > 0) {
            content = jdbcTemplate.query(dataSql, params, (rs, rowNum) -> {
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
        }

        int totalPages = (int) Math.ceil((double) (totalElements != null ? totalElements : 0) / pageSize);

        boolean isFirst = page == 0;
        boolean isLast = totalPages == 0 || page >= totalPages - 1;

        return new PageResult<>(
                content,
                page,
                pageSize,
                totalElements != null ? totalElements : 0L,
                totalPages,
                isFirst,
                isLast
        );
    }
}