package com.reservation.service.wallet;

import com.reservation.common.ApiMessage;
import com.reservation.handler.BusinessException;
import com.reservation.model.wallet.TransactionReferenceType;
import com.reservation.model.wallet.TransactionStatus;
import com.reservation.model.wallet.TransactionType;
import com.reservation.model.wallet.Wallet;
import com.reservation.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service handling core financial operations for the wallet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    /**
     * Fetches the wallet with a pessimistic lock. If the user doesn't have a wallet,
     * a new one is created with zero balance.
     */
    private Wallet getWalletForUpdateOrCreate(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    log.info("Creating a new wallet for userId: {}", userId);
                    return walletRepository.createWallet(userId);
                });
    }

    /**
     * Deducts the specified amount from the user's wallet.
     * Throws an exception if the wallet is inactive or has insufficient balance.
     */
    @Transactional
    public Long withdraw(Long userId, BigDecimal amount, TransactionReferenceType refType, Long refId, String description) {
        log.info("Withdrawing {} from wallet of userId: {}", amount, userId);

        // check the wallet row to prevent concurrent modifications
        Wallet wallet = getWalletForUpdateOrCreate(userId);

        // validate wallet status and balance
        if (!wallet.getIsActive()) {
            log.warn("Withdrawal failed: Wallet {} is inactive", wallet.getWalletId());
            throw new BusinessException(ApiMessage.WALLET_NOT_ACTIVE);
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Withdrawal failed: Insufficient balance. Current: {}, Requested: {}", wallet.getBalance(), amount);
            throw new BusinessException(ApiMessage.INSUFFICIENT_WALLET_BALANCE);
        }

        // update the balance
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        walletRepository.updateBalance(wallet.getWalletId(), newBalance);

        // insert transaction log
        return walletRepository.insertTransaction(
                wallet.getWalletId(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.SUCCESS,
                amount,
                newBalance,
                description,
                refType,
                refId
        );
    }

    /**
     * Adds the specified amount to the user's wallet.
     */
    @Transactional
    public Long deposit(Long userId, BigDecimal amount, TransactionReferenceType refType, Long refId, String description) {
        log.info("Depositing {} to wallet of userId: {}", amount, userId);

        // check the wallet row
        Wallet wallet = getWalletForUpdateOrCreate(userId);

        // check if wallet is active
        if (!wallet.getIsActive()) {
            log.warn("Deposit failed: Wallet {} is inactive", wallet.getWalletId());
            throw new BusinessException(ApiMessage.WALLET_NOT_ACTIVE);
        }

        // update the balance
        BigDecimal newBalance = wallet.getBalance().add(amount);
        walletRepository.updateBalance(wallet.getWalletId(), newBalance);

        // insert transaction log
        return walletRepository.insertTransaction(
                wallet.getWalletId(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                amount,
                newBalance,
                description,
                refType,
                refId
        );
    }
}