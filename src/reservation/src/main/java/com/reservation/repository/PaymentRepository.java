package com.reservation.repository;

import com.reservation.dto.PageResult;
import com.reservation.dto.payment.get.PaymentHistoryResponse;
import com.reservation.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PaymentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public record OrderWithReservation(Order order, Reservation reservation) {}

    public Optional<OrderWithReservation> getOrderWithReservation(Long orderId, Long userId) {
        String sql = """
            SELECT
                o.order_id, o.reservation_id, o.user_id AS order_user_id,
                o.total_amount, o.status AS order_status, o.created_at AS order_created_at,
                r.user_id AS res_user_id, r.created_at AS res_created_at,
                r.expires_at, r.status AS res_status
            FROM ticket_order o
            JOIN reservation r ON o.reservation_id = r.reservation_id
            WHERE o.order_id = :orderId AND o.user_id = :userId
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("userId", userId);

        try {
            OrderWithReservation result = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {

                Order order = Order.builder()
                        .orderId(rs.getLong("order_id"))
                        .reservationId(rs.getLong("reservation_id"))
                        .userId(rs.getLong("order_user_id"))
                        .totalAmount(rs.getBigDecimal("total_amount"))
                        .status(OrderStatus.valueOf(rs.getString("order_status")))
                        .createdAt(rs.getObject("order_created_at", OffsetDateTime.class))
                        .build();

                Reservation reservation = Reservation.builder()
                        .reservationId(rs.getLong("reservation_id"))
                        .userId(rs.getLong("res_user_id"))
                        .createdAt(rs.getObject("res_created_at", OffsetDateTime.class))
                        .expiresAt(rs.getObject("expires_at", OffsetDateTime.class))
                        .status(ReservationStatus.valueOf(rs.getString("res_status")))
                        .build();

                return new OrderWithReservation(order, reservation);
            });

            return Optional.ofNullable(result);

        } catch (EmptyResultDataAccessException e) {
            log.warn("Order {} not found for user {}", orderId, userId);
            return Optional.empty();
        }
    }

    /**
     * Make Payment record with pending status
     */
    public void createPayment(Long orderId, Integer methodId, BigDecimal amount, String token) {
        String sql = """
            INSERT INTO payment (order_id, method_id, amount, token, status)
            VALUES (:orderId, :methodId, :amount, :token, 'PENDING'::payment_status)
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("methodId", methodId)
                .addValue("amount", amount)
                .addValue("token", token);

        jdbcTemplate.update(sql, params);
        log.info("Inserted payment record for orderId: {} with token: {}", orderId, token);
    }

    public Optional<String> getPendingPaymentTokenByOrderId(Long orderId, Long userId) {
        String sql = """
            SELECT p.token
            FROM payment p
            JOIN ticket_order o ON p.order_id = o.order_id
            WHERE p.order_id = :orderId
              AND o.user_id = :userId
              AND p.status = 'PENDING'::payment_status
            LIMIT 1
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("userId", userId);

        try {
            String token = jdbcTemplate.queryForObject(sql, params, String.class);
            return Optional.ofNullable(token);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Mock Payment Gateway
     */
    public Optional<Payment> getPaymentByToken(String token) {
        String sql = """
            SELECT
                p.payment_id, p.order_id, p.amount, p.token, p.ref_id, p.paid_at, p.status,
                pm.method_id, pm.name AS method_name, pm.fee_percentage, pm.is_active
            FROM payment p
            JOIN payment_methods pm ON p.method_id = pm.method_id
            WHERE p.token = :token
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("token", token);

        try {
            Payment payment = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {

                PaymentMethod paymentMethod = PaymentMethod.builder()
                        .methodId(rs.getInt("method_id"))
                        .name(rs.getString("method_name"))
                        .feePercentage(rs.getBigDecimal("fee_percentage"))
                        .isActive(rs.getBoolean("is_active"))
                        .build();

                return Payment.builder()
                        .paymentId(rs.getLong("payment_id"))
                        .orderId(rs.getLong("order_id"))
                        .method(paymentMethod)
                        .amount(rs.getBigDecimal("amount"))
                        .token(rs.getString("token"))
                        .refId(rs.getString("ref_id"))
                        .paidAt(rs.getObject("paid_at", OffsetDateTime.class))
                        .status(PaymentStatus.valueOf(rs.getString("status")))
                        .build();
            });

            return Optional.ofNullable(payment);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public record PaymentWithOrderAndReservation(
            Payment payment,
            Order order,
            Reservation reservation
    ) {}

    public Optional<PaymentWithOrderAndReservation> getPaymentWithOrderAndReservation(String token, Long userId) {
        String sql = """
            SELECT
                p.payment_id, p.order_id, p.amount, p.token, p.ref_id, p.paid_at, p.status AS payment_status,
                pm.method_id, pm.name AS method_name, pm.fee_percentage, pm.is_active,
                o.reservation_id, o.user_id AS order_user_id, o.total_amount, o.status AS order_status, o.created_at AS order_created_at,
                r.user_id AS res_user_id, r.created_at AS res_created_at, r.expires_at, r.status AS res_status
            FROM payment p
            JOIN payment_methods pm ON p.method_id = pm.method_id
            JOIN ticket_order o ON p.order_id = o.order_id
            JOIN reservation r ON o.reservation_id = r.reservation_id
            WHERE p.token = :token AND o.user_id = :userId
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("token", token)
                .addValue("userId", userId);

        try {
            PaymentWithOrderAndReservation result = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {

                PaymentMethod paymentMethod = PaymentMethod.builder()
                        .methodId(rs.getInt("method_id"))
                        .name(rs.getString("method_name"))
                        .feePercentage(rs.getBigDecimal("fee_percentage"))
                        .isActive(rs.getBoolean("is_active"))
                        .build();

                Payment payment = Payment.builder()
                        .paymentId(rs.getLong("payment_id"))
                        .orderId(rs.getLong("order_id"))
                        .method(paymentMethod)
                        .amount(rs.getBigDecimal("amount"))
                        .token(rs.getString("token"))
                        .refId(rs.getString("ref_id"))
                        .paidAt(rs.getObject("paid_at", OffsetDateTime.class))
                        .status(PaymentStatus.valueOf(rs.getString("payment_status")))
                        .build();

                Order order = Order.builder()
                        .orderId(rs.getLong("order_id"))
                        .reservationId(rs.getLong("reservation_id"))
                        .userId(rs.getLong("order_user_id"))
                        .totalAmount(rs.getBigDecimal("total_amount"))
                        .status(OrderStatus.valueOf(rs.getString("order_status")))
                        .createdAt(rs.getObject("order_created_at", OffsetDateTime.class))
                        .build();

                Reservation reservation = Reservation.builder()
                        .reservationId(rs.getLong("reservation_id"))
                        .userId(rs.getLong("res_user_id"))
                        .createdAt(rs.getObject("res_created_at", OffsetDateTime.class))
                        .expiresAt(rs.getObject("expires_at", OffsetDateTime.class))
                        .status(ReservationStatus.valueOf(rs.getString("res_status")))
                        .build();

                return new PaymentWithOrderAndReservation(payment, order, reservation);
            });

            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void updatePaymentStatus(String token, PaymentStatus status, String refId) {
        String sql = """
            UPDATE payment
            SET status = :status::payment_status,
                ref_id = :refId,
                paid_at = CURRENT_TIMESTAMP
            WHERE token = :token
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("refId", refId)
                .addValue("token", token);

        jdbcTemplate.update(sql, params);
    }

    /**
     * Fetch user payment history with pagination and optional status filter.
     */
    public PageResult<PaymentHistoryResponse> getUserPaymentHistory(
            Long userId, int page, int pageSize, PaymentStatus status) {

        String statusCondition = (status != null) ? " AND p.status = :status::payment_status " : "";

        String countSql = """
        SELECT COUNT(p.payment_id)
        FROM payment p
        INNER JOIN ticket_order o ON p.order_id = o.order_id
        WHERE o.user_id = :user_id
        """ + statusCondition;

        String dataSql = """
        SELECT p.payment_id, p.order_id, p.amount, p.status, p.paid_at
        FROM payment p
        INNER JOIN ticket_order o ON p.order_id = o.order_id
        WHERE o.user_id = :user_id
        """ + statusCondition + """
        ORDER BY p.paid_at DESC NULLS LAST, p.payment_id DESC
        LIMIT :limit OFFSET :offset
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("limit", pageSize)
                .addValue("offset", page * pageSize);

        if (status != null) {
            params.addValue("status", status.name());
        }

        Long totalElements = jdbcTemplate.queryForObject(countSql, params, Long.class);

        List<PaymentHistoryResponse> content = Collections.emptyList();
        if (totalElements != null && totalElements > 0) {
            content = jdbcTemplate.query(dataSql, params, (rs, rowNum) ->
                    PaymentHistoryResponse.builder()
                            .paymentId(rs.getLong("payment_id"))
                            .orderId(rs.getLong("order_id"))
                            .amount(rs.getBigDecimal("amount"))
                            .status(rs.getString("status"))
                            .paidAt(rs.getObject("paid_at", OffsetDateTime.class))
                            .build()
            );
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

    /**
     * Finds detailed payment information mapping to the Payment model.
     * Inner joins with ticket_order to verify user ownership.
     */
    public Optional<Payment> findUserPaymentById(Long paymentId, Long userId) {
        String sql = """
            SELECT p.payment_id, p.order_id, p.amount, p.ref_id, p.paid_at, p.status,
                   pm.method_id, pm.name as method_name, pm.fee_percentage
            FROM payment p
            INNER JOIN ticket_order o ON p.order_id = o.order_id
            INNER JOIN payment_methods pm ON p.method_id = pm.method_id
            WHERE p.payment_id = :payment_id AND o.user_id = :user_id
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("payment_id", paymentId)
                .addValue("user_id", userId);

        try {
            Payment payment = jdbcTemplate.queryForObject(
                    sql,
                    params,
                    (rs, rowNum) -> {
                        PaymentMethod method = PaymentMethod.builder()
                                .methodId(rs.getInt("method_id"))
                                .name(rs.getString("method_name"))
                                .feePercentage(rs.getBigDecimal("fee_percentage"))
                                .build();

                        return Payment.builder()
                                .paymentId(rs.getLong("payment_id"))
                                .orderId(rs.getLong("order_id"))
                                .amount(rs.getBigDecimal("amount"))
                                .refId(rs.getString("ref_id"))
                                .status(PaymentStatus.valueOf(rs.getString("status")))
                                .paidAt(rs.getObject("paid_at", OffsetDateTime.class))
                                .method(method)
                                .build();
                    }
            );
            return Optional.ofNullable(payment);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}