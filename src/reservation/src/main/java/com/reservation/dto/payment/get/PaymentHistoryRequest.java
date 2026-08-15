package com.reservation.dto.payment.get;

import com.reservation.model.PaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentHistoryRequest(

        @NotNull(message = "FIELD_EMPTY")
        @Min(value = 0, message = "INVALID_PAGE_AMOUNT")
        Integer page,

        @NotNull(message = "FIELD_EMPTY")
        @Min(value = 1, message = "INVALID_PAGE_SIZE_AMOUNT")
        @Max(value = 50, message = "INVALID_PAGE_SIZE_AMOUNT")
        Integer page_size,

        PaymentStatus status
) {}