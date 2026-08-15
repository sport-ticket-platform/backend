package com.reservation.dto.wallet.get;

import com.reservation.model.wallet.TransactionStatus;
import com.reservation.model.wallet.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletInfoRequest(

        @NotNull(message = "FIELD_EMPTY")
        @Min(value = 0, message = "INVALID_PAGE_AMOUNT")
        Integer page,

        @NotNull(message = "FIELD_EMPTY")
        @Min(value = 1, message = "INVALID_PAGE_SIZE_AMOUNT")
        @Max(value = 50, message = "INVALID_PAGE_SIZE_AMOUNT")
        Integer page_size,

        TransactionType type,
        TransactionStatus status
) {}