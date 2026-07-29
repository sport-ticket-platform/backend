package com.reservation.repository;

import com.reservation.dto.grpc.ReservedSeatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReservationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ReservedSeatDTO> mapper = (rs, rowNum) -> ReservedSeatDTO.builder()
            .seatId(rs.getLong("seat_id"))
            .configId(rs.getInt("config_id"))
            .build();

    public List<ReservedSeatDTO> getReservedSeats(List<Integer> configIds) {
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

        return jdbcTemplate.query(sql, params, mapper);
    }
}