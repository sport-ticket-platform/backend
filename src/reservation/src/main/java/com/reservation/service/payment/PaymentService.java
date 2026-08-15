package com.reservation.service.payment;

import com.reservation.common.ApiMessage;
import com.reservation.dto.PageResult;
import com.reservation.dto.payment.PaymentCallbackRequest;
import com.reservation.dto.payment.PaymentCallbackResponse;
import com.reservation.dto.payment.PaymentResponse;
import com.reservation.dto.payment.get.PaymentHistoryRequest;
import com.reservation.dto.payment.get.PaymentHistoryResponse;
import com.reservation.dto.wallet.WalletPaymentResponse;
import com.reservation.handler.BusinessException;
import com.reservation.model.*;
import com.reservation.model.wallet.TransactionReferenceType;
import com.reservation.repository.PaymentRepository;
import com.reservation.service.order.OrderService;
import com.reservation.service.reservation.ReservationService;
import com.reservation.service.wallet.WalletService;
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
    private final WalletService walletService;

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
                    .orderId(order.getOrderId())
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

            // 1. Increase user balance (Refund to wallet)
            walletService.deposit(
                    userId,
                    payment.getAmount(),
                    TransactionReferenceType.PAYMENT,
                    payment.getPaymentId(),
                    "Refund for expired reservation of order: " + order.getOrderId()
            );

            // 2. Update payment status to REFUNDED
            paymentRepository.updatePaymentStatus(request.token(), PaymentStatus.REFUNDED, refId);

            return PaymentCallbackResponse.builder()
                    .refId(refId)
                    .orderId(order.getOrderId())
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
                .orderId(order.getOrderId())
                .build();
    }

    @Transactional
    public WalletPaymentResponse payWithWallet(Long orderId, Long userId) {
        log.info("Initiating wallet payment for orderId: {} by userId: {}", orderId, userId);

        PaymentRepository.OrderWithReservation info = paymentRepository.getOrderWithReservation(orderId, userId)
                .orElseThrow(() -> new BusinessException(ApiMessage.ORDER_NOT_FOUND_OR_NOT_YOURS));

        Order order = info.order();
        Reservation reservation = info.reservation();

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Wallet payment failed: Order {} is not PENDING (Status: {})", orderId, order.getStatus());
            throw new BusinessException(ApiMessage.ORDER_NOT_PENDING_FOR_PAYMENT);
        }
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            log.warn("Wallet payment failed: Reservation {} is not ACTIVE", reservation.getReservationId());
            throw new BusinessException(ApiMessage.RESERVATION_NOT_ACTIVE);
        }
        if (reservation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Wallet payment failed: Reservation {} is expired", reservation.getReservationId());
            throw new BusinessException(ApiMessage.RESERVATION_EXPIRED);
        }

        Long transactionId = walletService.withdraw(
                userId,
                order.getTotalAmount(),
                TransactionReferenceType.TICKET_ORDER,
                orderId,
                "payment for order: " + orderId
        );

        // Payment method for wallet is 2 in db initial data
        Integer walletMethodId = 2;
        String token = UUID.randomUUID().toString();
        String refId = "TRX-" + transactionId;

        paymentRepository.createPayment(orderId, walletMethodId, order.getTotalAmount(), token);
        paymentRepository.updatePaymentStatus(token, PaymentStatus.SUCCEEDED, refId);

        orderService.markOrderAsPaid(order.getOrderId());
        reservationService.completeReservationAndIssueTickets(reservation.getReservationId(), order.getOrderId());

        log.info("Wallet payment successful for orderId: {}. Tickets issued.", orderId);

        return WalletPaymentResponse.builder()
                .refId(refId)
                .orderId(order.getOrderId())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResult<PaymentHistoryResponse> getUserPaymentHistory(Long userId, PaymentHistoryRequest request) {

        log.info("Fetching payment history for userId: {}, page: {}, pageSize: {}",
                userId, request.page(), request.page_size());

        return paymentRepository.getUserPaymentHistory(
                userId,
                request.page(),
                request.page_size(),
                request.status()
        );
    }
}