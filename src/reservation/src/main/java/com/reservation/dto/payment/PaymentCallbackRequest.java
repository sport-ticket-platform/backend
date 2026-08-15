package com.reservation.dto.payment;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaymentCallbackRequest(

        @NotBlank(message = "FIELD_EMPTY")
        String token,

        @NotBlank(message = "PAYMENT_STATUS_INVALID")
        @Pattern(regexp = "^(SUCCESS|FAILED)$", message = "PAYMENT_STATUS_INVALID")
        String status
) {}