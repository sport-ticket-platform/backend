package com.reservation.dto.payment;

import lombok.Builder;

@Builder
public record PaymentResponse(
        String token
) {}
