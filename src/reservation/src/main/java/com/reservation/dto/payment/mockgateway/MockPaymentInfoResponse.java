package com.reservation.dto.payment.mockgateway;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MockPaymentInfoResponse(
        Long orderId,
        BigDecimal purchase_amount,
        BigDecimal percentage,
        BigDecimal percentage_amount,
        BigDecimal total_amount
) {}