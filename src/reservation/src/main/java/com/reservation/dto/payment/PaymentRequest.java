package com.reservation.dto.payment;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull(message = "FIELD_EMPTY")
        Long order_id
) {}
