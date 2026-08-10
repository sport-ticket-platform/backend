package com.reservation.service.payment;

import com.reservation.common.ApiMessage;
import com.reservation.dto.payment.PaymentCallbackRequest;
import com.reservation.dto.payment.PaymentCallbackResponse;
import com.reservation.dto.payment.PaymentResponse;
import com.reservation.handler.BusinessException;
import com.reservation.model.*;
import com.reservation.repository.PaymentRepository;
import com.reservation.service.order.OrderService;
import com.reservation.service.reservation.ReservationService;
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
    private final OrderService orderService;
    private final ReservationService reservationService;

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
            throw new BusinessException(ApiMessage.ORDER_NOT_PENDING_FOR_PAYMENT);
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

    @Transactional
    public PaymentCallbackResponse processCallback(PaymentCallbackRequest request, Long userId) {
        log.info("Processing payment callback for token: {} with status: {} by userId: {}",
                request.token(), request.status(), userId);

        PaymentRepository.PaymentWithOrderAndReservation info = paymentRepository
                .getPaymentWithOrderAndReservation(request.token(), userId)
                .orElseThrow(() -> new BusinessException(ApiMessage.INVALID_PAYMENT_TOKEN));

        Payment payment = info.payment();
        Order order = info.order();
        Reservation reservation = info.reservation();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment callback failed: Payment for token {} is already processed", request.token());
            throw new BusinessException(ApiMessage.PAYMENT_ALREADY_PROCESSED);
        }

        if ("FAILED".equalsIgnoreCase(request.status())) {
            log.info("Payment marked as FAILED for token: {}", request.token());
            paymentRepository.updatePaymentStatus(request.token(), PaymentStatus.FAILED, null);
            return PaymentCallbackResponse.builder()
                    .refId(null)
                    .build();
        }

        // ========================================

        String refId = String.valueOf((long) (Math.random() * 1_000_000_000_000L));

        boolean isReservationNotEnable = reservation.getStatus() != ReservationStatus.ACTIVE ||
                reservation.getExpiresAt().isBefore(OffsetDateTime.now());

        boolean isOrderNotPending = order.getStatus() != OrderStatus.PENDING;

        if (isReservationNotEnable || isOrderNotPending) {
            log.info(
                    "Order or Reservation is invalid (order status: {} | reserve status: {}). Refunding amount {} to userId {}",
                    order.getStatus(),
                    reservation.getStatus(),
                    payment.getAmount(),
                    userId
            );

            // TODO: 1. Increase user balance

            // 2. Update payment status to REFUNDED
            paymentRepository.updatePaymentStatus(request.token(), PaymentStatus.REFUNDED, refId);

            return PaymentCallbackResponse.builder()
                    .refId(refId)
                    .build();
        }

        // everything is fine, we can complete the order and reservation
        log.info("Reservation active. Completing payment and order {} for userId {}",
                order.getOrderId(), userId);

        paymentRepository.updatePaymentStatus(request.token(), PaymentStatus.SUCCEEDED, refId);
        orderService.markOrderAsPaid(order.getOrderId());
        reservationService.completeReservationAndIssueTickets(reservation.getReservationId(), order.getOrderId());

        return PaymentCallbackResponse.builder()
                .refId(refId)
                .build();
    }
}