package com.reservation.model.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    private Long walletId;
    private Long userId;
    private BigDecimal balance;
    private Boolean isActive;
}