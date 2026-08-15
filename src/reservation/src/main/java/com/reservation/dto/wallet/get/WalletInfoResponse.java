package com.reservation.dto.wallet.get;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.reservation.dto.PageResult;
import com.reservation.model.wallet.WalletTransaction;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WalletInfoResponse(
        BigDecimal balance,
        Boolean isActive,
        PageResult<WalletTransaction> transactions
) {}