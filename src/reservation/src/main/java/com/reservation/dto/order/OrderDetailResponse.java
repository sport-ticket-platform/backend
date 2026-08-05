package com.reservation.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.reservation.model.Order;
import com.reservation.model.ReservationSeat;
import com.reservation.model.SoldSeat;
import lombok.Builder;

import java.util.List;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderDetailResponse(
        Order order,
        Long matchId,
        List<SoldSeat> soldSeats,
        List<ReservationSeat> reservationSeats
) {}