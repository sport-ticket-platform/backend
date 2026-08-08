package com.reservation.repository;

import com.reservation.model.Order;
import com.reservation.model.OrderStatus;
import com.reservation.model.Reservation;
import com.reservation.model.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    /**
     * پیدا کردن توکن پرداخت فعال (PENDING) برای یک سفارش، با اعتبارسنجی مالکیت کاربر
     */
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
}