package com.reservation.service.mockgateway;

import com.reservation.common.ApiMessage;
import com.reservation.dto.payment.mockgateway.MockPaymentInfoResponse;
import com.reservation.handler.BusinessException;
import com.reservation.model.Payment;
import com.reservation.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Provides business logic for Mock Gateway Payment.
 *
 * @author logTAHA
 * @since 1.0.0
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockPaymentGatewayService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public MockPaymentInfoResponse getMockPaymentInfo(String token) {
        log.info("Fetching mock payment info for token: {}", token);

        Payment payment = paymentRepository.getPaymentByToken(token)
                .orElseThrow(() -> {
                    log.warn("Payment record not found for token: {}", token);
                    return new BusinessException(ApiMessage.INVALID_PAYMENT_TOKEN);
                });

        BigDecimal purchaseAmount = payment.getAmount();
        BigDecimal percentage = payment.getMethod().getFeePercentage();
        percentage = percentage != null ? percentage : BigDecimal.ZERO;

        BigDecimal percentageAmount = purchaseAmount.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = purchaseAmount.add(percentageAmount);

        return MockPaymentInfoResponse.builder()
                .orderId(payment.getOrderId())
                .purchase_amount(purchaseAmount)
                .percentage(percentage)
                .percentage_amount(percentageAmount)
                .total_amount(totalAmount)
                .build();
    }
}