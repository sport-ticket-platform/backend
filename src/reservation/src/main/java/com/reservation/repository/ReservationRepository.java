package com.reservation.repository;

import com.reservation.dto.grpc.ReservedSeatDTO;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
public class ReservationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ReservedSeatDTO> reservedSeatMapper = (rs, rowNum) -> ReservedSeatDTO.builder()
            .seatId(rs.getLong("seat_id"))
            .configId(rs.getInt("config_id"))
            .build();

    // GRPC
    public List<ReservedSeatDTO> getReservedSeatsByConfigIds(List<Integer> configIds) {
        if (configIds == null || configIds.isEmpty()) {
            log.warn("Empty configIds list passed to getUnavailableSeats");
            return Collections.emptyList();
        }

        String sql = """
            SELECT seat_id, config_id
            FROM reservation_seat
            WHERE config_id IN (:configIds) AND is_active = true
            
            UNION
            
            SELECT st.seat_id, s.config_id
            FROM sold_ticket st
            JOIN seat s ON st.seat_id = s.seat_id
            WHERE s.config_id IN (:configIds) AND st.status = 'VALID'::ticket_status
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("configIds", configIds);

        return jdbcTemplate.query(sql, params, reservedSeatMapper);
    }


    // ======================================================================
    //                     Reserve Validation
    // ======================================================================

    /**
     * @param seatIds: list of seat ids in Long
     * @return a list of seat ids that not exist in db(in Long format)
     */
    public List<Long> findNonExistSeatIds(List<Long> seatIds) {
        String sql = """
            SELECT unnest(ARRAY[:seat_ids]::bigint[])
            EXCEPT
            SELECT seat_id FROM seat WHERE seat_id IN (:seat_ids)
            """;

        Map<String, Object> params = Map.of("seat_ids", seatIds);

        return jdbcTemplate.queryForList(sql, params, Long.class);
    }


    /**
     * Returns the single {@code match_id} if all specified seats exist and belong to the exact same match.
     *
     * @param seatIds list of seat IDs to validate; must not be null or empty
     * @return an {@link Optional} containing the {@code match_id}, or {@link Optional#empty()} if seats
     *         belong to multiple matches or do not exist
     */
    public Optional<Long> findSingleMatchIdForSeats(List<Long> seatIds) {
        String sql = """
            SELECT tc.match_id FROM seat s
            JOIN ticket_config tc ON s.config_id= tc.config_id
            WHERE s.seat_id IN (:seat_ids)
            GROUP BY tc.match_id
            HAVING count(s.seat_id) = cardinality(ARRAY[:seat_ids]::bigint[])
            """;

        try {
            Map<String, Object> params = Map.of("seat_ids", seatIds);
            Long matchId = jdbcTemplate.queryForObject(sql, params, Long.class);
            return Optional.ofNullable(matchId);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public record MatchStatus(boolean isAvailable, boolean isPaymentOpen) {}

    public Optional<MatchStatus> getMatchStatus(Long matchId) {
        String sql = """
            SELECT
                (match_time > CURRENT_TIMESTAMP) AS is_available,
                is_payment_open
            FROM "match"
            WHERE match_id = :match_id
        """;

        Map<String, Object> params = Map.of("match_id", matchId);

        try {
            MatchStatus status = jdbcTemplate.queryForObject(
                    sql,
                    params,
                    (rs, rowNum) -> new MatchStatus(
                            rs.getBoolean("is_available"),
                            rs.getBoolean("is_payment_open")
                    )
            );
            return Optional.ofNullable(status);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns a list of seat IDs that are currently unavailable (actively reserved or sold).
     *
     * @param seatIds list of seat IDs to check; returns empty list if null or empty
     * @return list of unavailable seat IDs
     */
    public List<Long> findNotAvailableSeatIds(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }

        String sql = """
        SELECT rs.seat_id
        FROM reservation_seat rs
        JOIN reservation r ON rs.reservation_id = r.reservation_id
        WHERE rs.seat_id IN (:seat_ids)
          AND rs.is_active = TRUE
          AND r.status = 'ACTIVE'::reservation_status
          AND r.expires_at > CURRENT_TIMESTAMP

        UNION

        SELECT st.seat_id
        FROM sold_ticket st
        WHERE st.seat_id IN (:seat_ids)
          AND st.status = 'VALID'::ticket_status
        """;

        Map<String, Object> params = Map.of("seat_ids", seatIds);
        return jdbcTemplate.queryForList(sql, params, Long.class);
    }


    // ======================================================================
    //                     Reserve Process
    // ======================================================================

    public record SeatConfigInfo(Long seatId, Integer configId, BigDecimal price) {}

    /**
     * Returns config and price details for the specified seats.
     *
     * @param seatIds list of seat IDs
     * @return list of seat configuration info
     */
    public List<SeatConfigInfo> findAllSeatConfigInfo(List<Long> seatIds) {
        String sql = """
            SELECT s.seat_id ,s.config_id, tc.price FROM seat s
            JOIN ticket_config tc ON s.config_id = tc.config_id
            WHERE s.seat_id IN (:seat_ids)
            """;

        return jdbcTemplate.query(
                sql,
                Map.of("seat_ids", seatIds),
                (rs, rowNum) -> new SeatConfigInfo(
                        rs.getLong("seat_id"),
                        rs.getInt("config_id"),
                        rs.getBigDecimal("price")
                )
        );
    }

    /**
     * Creates a new reservation record and returns the generated reservation ID.
     */
    public Long createReservation(Long userId, OffsetDateTime expiresAt) {

        String sql = """
            INSERT INTO reservation (user_id, expires_at)
            VALUES (:user_id, :expires_at)
            """;

        MapSqlParameterSource paramMap =  new MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("expires_at", expiresAt);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                sql,
                paramMap,
                keyHolder,
                new String[]{"reservation_id"}
        );

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated reservation_id");
        }

        return key.longValue();
    }

    /**
     * Inserts multiple seats into the reservation_seat table using a batch update.
     */
    public void insertReservationSeats(Long reservationId, List<SeatConfigInfo> seatInfos) {
        String sql = """
            INSERT INTO reservation_seat (reservation_id, config_id, seat_id)
            VALUES (:reservation_id, :config_id, :seat_id)
            """;

        SqlParameterSource[] batchParams = seatInfos.stream()
                .map(info -> new MapSqlParameterSource()
                        .addValue("reservation_id", reservationId)
                        .addValue("config_id", info.configId())
                        .addValue("seat_id", info.seatId())
                )
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    public Long createTicketOrder(Long reservationId, Long userId, BigDecimal totalPrice) {
        String sql = """
            INSERT INTO ticket_order (reservation_id, user_id, total_amount)
            VALUES (:reservation_id, :user_id, :total_amount)
            """;

        MapSqlParameterSource paramMap = new MapSqlParameterSource()
                .addValue("reservation_id", reservationId)
                .addValue("user_id", userId)
                .addValue("total_amount", totalPrice);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                sql,
                paramMap,
                keyHolder,
                new String[]{"order_id"}
        );

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated reservation_id");
        }

        return key.longValue();
    }


    // ======================================================================
    //                         Cancel Reservation
    // ======================================================================

    /**
     * Expire an active reservation.
     * <p>reservation table status: EXPIRED</p>
     * <p>reservation_seat, each one: is_active = false</p>
     * <p>ticket_order, status: FAILED</p>
     */
    public void expireReservation(Long reservationId) {
        String sql = """
        WITH release_seats AS (
            UPDATE reservation_seat
            SET is_active = false
            WHERE reservation_id = :reservation_id
        ),
        expire_order AS (
            UPDATE ticket_order
            SET status = 'FAILED'
            WHERE reservation_id = :reservation_id AND status = 'PENDING'
        )
        UPDATE reservation
        SET status = 'EXPIRED'::reservation_status
        WHERE reservation_id = :reservation_id AND status = 'ACTIVE'::reservation_status
        """;

        jdbcTemplate.update(sql, Map.of("reservation_id", reservationId));
    }
}