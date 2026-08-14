package com.reservation.dto.wallet;

import jakarta.validation.constraints.NotNull;

public record WalletPaymentRequest(

        @NotNull(message = "FIELD_EMPTY")
        Long order_id
) {}