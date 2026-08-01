package com.reservation.dto.grpc;

import lombok.Builder;

@Builder
public record ReservedSeatDTO(
        Long seatId,
        Integer configId
) {}