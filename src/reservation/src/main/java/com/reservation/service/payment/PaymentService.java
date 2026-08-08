package com.reservation.service.payment;

import com.reservation.common.ApiMessage;
import com.reservation.dto.payment.PaymentResponse;
import com.reservation.handler.BusinessException;
import com.reservation.model.Order;
import com.reservation.model.OrderStatus;
import com.reservation.model.Reservation;
import com.reservation.model.ReservationStatus;
import com.reservation.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides business logic for Payment.
 *
 * @author logTAHA
 * @since 1.0.0
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse initiatePayment(Long orderId, Long userId) {
        log.info("Initiating payment for orderId: {} by userId: {}", orderId, userId);

        // get order and reservation
        PaymentRepository.OrderWithReservation info = paymentRepository.getOrderWithReservation(orderId, userId)
                .orElseThrow(() -> new BusinessException(ApiMessage.ORDER_NOT_FOUND_OR_NOT_YOURS));

        Order order = info.order();
        Reservation reservation = info.reservation();
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Payment failed: Order {} is not PENDING (Status: {})", orderId, order.getStatus());
            throw new BusinessException(ApiMessage.ORDER_NOT_PENDING);
        }
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            log.warn("Payment failed: Reservation {} is not ACTIVE (Status: {})", reservation.getReservationId(), reservation.getStatus());
            throw new BusinessException(ApiMessage.RESERVATION_NOT_ACTIVE);
        }
        if (reservation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Payment failed: Reservation {} is expired", reservation.getReservationId());
            throw new BusinessException(ApiMessage.RESERVATION_EXPIRED);
        }

        // checking that order has active payment?
        Optional<String> existingToken = paymentRepository.getPendingPaymentTokenByOrderId(orderId, userId);
        if (existingToken.isPresent()) {
            log.info("Found existing PENDING payment for orderId: {}. Reusing token: {}", orderId, existingToken.get());
            return PaymentResponse.builder()
                    .token(existingToken.get())
                    .build();
        }

        String token = UUID.randomUUID().toString();

        Integer mockMethodId = 1;
        paymentRepository.createPayment(orderId, mockMethodId, order.getTotalAmount(), token);

        return PaymentResponse.builder()
                .token(token)
                .build();
    }
}