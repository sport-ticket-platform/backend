package com.reservation.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderCancelPenaltyResponse(
        Long orderId,
        BigDecimal totalAmount,
        BigDecimal penaltyAmount,
        BigDecimal refundableAmount,
        Integer penaltyPercentage,
        Boolean isCancellable
) {}