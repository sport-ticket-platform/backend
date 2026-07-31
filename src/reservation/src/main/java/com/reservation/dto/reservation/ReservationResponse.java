package com.reservation.dto.reservation;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ReservationResponse(
        Long reservation_id,
        OffsetDateTime expires_at
) {
}