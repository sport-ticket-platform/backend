package com.reservation.dto.reservation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReservationRequest(
        @NotNull(message = "RESERVE_SEAT_IDS_EMPTY")
        @NotEmpty(message = "RESERVE_SEAT_IDS_EMPTY")
        List<Long> seat_ids
) {
}