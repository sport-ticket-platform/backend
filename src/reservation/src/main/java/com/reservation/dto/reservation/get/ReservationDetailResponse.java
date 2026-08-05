package com.reservation.dto.reservation.get;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.reservation.model.Reservation;
import com.reservation.model.ReservationSeat;
import lombok.Builder;

import java.util.List;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationDetailResponse(
        Reservation reservation,
        Long orderId,
        Long matchId,
        List<ReservationSeat> reservationSeats
) {}