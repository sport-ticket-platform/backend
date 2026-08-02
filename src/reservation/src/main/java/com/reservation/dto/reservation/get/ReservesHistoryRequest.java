package com.reservation.dto.reservation.get;

import com.reservation.model.ReservationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReservesHistoryRequest(

        @NotNull(message = "FIELD_EMPTY")
        @Min(value = 0, message = "GET_RESERVES_PAGE_AMOUNT")
        Integer page,

        @NotNull(message = "FIELD_EMPTY")
        @Min(value = 1, message = "GET_RESERVES_PAGE_SIZE_AMOUNT")
        @Max(value = 50, message = "GET_RESERVES_PAGE_SIZE_AMOUNT")
        Integer page_size,

        ReservationStatus status
) {}