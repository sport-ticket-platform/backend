package com.reservation.repository;

import com.reservation.dto.PageResult;
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
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Fetch user order history with pagination and optional status filter.
     */
    public PageResult<Order> getUserOrderHistory(Long userId, int page, int pageSize, OrderStatus status) {

        String statusCondition = (status != null) ? " AND status = :status::order_status " : "";

        String countSql = """
            SELECT COUNT(*)
            FROM ticket_order
            WHERE user_id = :user_id
            """ + statusCondition;

        String dataSql = """
            SELECT order_id, reservation_id, user_id, total_amount, status, created_at
            FROM ticket_order
            WHERE user_id = :user_id
            """ + statusCondition + """
            ORDER BY created_at DESC
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

        List<Order> content = Collections.emptyList();
        if (totalElements != null && totalElements > 0) {
            content = jdbcTemplate.query(dataSql, params, (rs, rowNum) -> {
                String rsStatus = rs.getString("status");

                return Order.builder()
                        .orderId(rs.getLong("order_id"))
                        .reservationId(rs.getObject("reservation_id", Long.class))
                        .userId(rs.getLong("user_id"))
                        .totalAmount(rs.getBigDecimal("total_amount"))
                        .status(OrderStatus.valueOf(rsStatus))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
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


    /**
     * Finds an order by its ID and User ID. Intended for user-facing endpoints.
     */
    public Optional<Order> findUserOrderById(Long orderId, Long userId) {
        String sql = """
            SELECT order_id, reservation_id, user_id, total_amount, status, created_at
            FROM ticket_order
            WHERE order_id = :order_id AND user_id = :user_id
            """;

        Map<String, Object> params = Map.of(
                "order_id", orderId,
                "user_id", userId
        );

        try {
            Order order = jdbcTemplate.queryForObject(
                    sql,
                    params,
                    (rs, rowNum) -> Order.builder()
                            .orderId(rs.getLong("order_id"))
                            .reservationId(rs.getObject("reservation_id", Long.class))
                            .userId(rs.getLong("user_id"))
                            .totalAmount(rs.getBigDecimal("total_amount"))
                            .status(OrderStatus.valueOf(rs.getString("status")))
                            .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                            .build()
            );
            return Optional.ofNullable(order);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Fetches seat and config details for sold tickets of a specific order.
     */
    public List<SoldSeat> findUserSoldTicketDetails(Long orderId) {
        String sql = """
            SELECT
                st.ticket_id, st.order_id, st.price, st.status, st.created_at, st.updated_at,
                s.seat_id, s.section, s.row_no, s.seat_no,
                tc.config_id, tc.match_id,
                cat.category_id, cat.name as category_name
            FROM sold_ticket st
                JOIN seat s ON st.seat_id = s.seat_id
                JOIN ticket_config tc ON s.config_id = tc.config_id
                JOIN ticket_category cat ON tc.category_id = cat.category_id
            WHERE st.order_id = :order_id
            """;

        Map<String, Object> params = Map.of("order_id", orderId);

        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> {
                    // Mapping TicketCategory
                    TicketCategory category = TicketCategory.builder()
                            .categoryId(rs.getInt("category_id"))
                            .name(rs.getString("category_name"))
                            .build();

                    // Mapping TicketConfig
                    TicketConfig config = TicketConfig.builder()
                            .configId(rs.getInt("config_id"))
                            .matchId(rs.getLong("match_id"))
                            .category(category)
                            .build();

                    // Mapping SoldSeat
                    return SoldSeat.builder()
                            .ticketId(rs.getLong("ticket_id"))
                            .orderId(rs.getLong("order_id"))
                            .price(rs.getBigDecimal("price"))
                            .status(TicketStatus.valueOf(rs.getString("status")))
                            .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                            .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                            .seatId(rs.getLong("seat_id"))
                            .section(rs.getInt("section"))
                            .rowNo(rs.getInt("row_no"))
                            .seatNo(rs.getInt("seat_no"))
                            .ticketConfig(config)
                            .build();
                }
        );
    }

    /**
     * Fetches seat and config details from reservation_seat for PENDING/FAILED orders.
     */
    public List<ReservationSeat> findUserReservationSeatsByOrderId(Long orderId) {
        String sql = """
            SELECT
                rs.reservation_id,
                s.seat_id, s.section, s.row_no, s.seat_no,
                tc.config_id, tc.match_id, tc.price,
                cat.category_id, cat.name as category_name
            FROM ticket_order ord
                JOIN reservation_seat rs ON ord.reservation_id = rs.reservation_id
                JOIN seat s ON rs.seat_id = s.seat_id
                JOIN ticket_config tc ON s.config_id = tc.config_id
                JOIN ticket_category cat ON tc.category_id = cat.category_id
            WHERE ord.order_id = :order_id
            """;

        Map<String, Object> params = Map.of("order_id", orderId);

        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> {
                    // Mapping TicketCategory
                    TicketCategory category = TicketCategory.builder()
                            .categoryId(rs.getInt("category_id"))
                            .name(rs.getString("category_name"))
                            .build();

                    // Mapping TicketConfig
                    TicketConfig config = TicketConfig.builder()
                            .configId(rs.getInt("config_id"))
                            .matchId(rs.getLong("match_id"))
                            .price(rs.getBigDecimal("price"))
                            .category(category)
                            .build();

                    // Mapping ReservationSeat
                    return ReservationSeat.builder()
                            .reservationId(rs.getLong("reservation_id"))
                            .seatId(rs.getLong("seat_id"))
                            .section(rs.getInt("section"))
                            .rowNo(rs.getInt("row_no"))
                            .seatNo(rs.getInt("seat_no"))
                            .ticketConfig(config)
                            .build();
                }
        );
    }
}