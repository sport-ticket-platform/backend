package com.reservation.model.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {
    private Long transactionId;
    private Long walletId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private TransactionReferenceType referenceType;
    private Long referenceId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}