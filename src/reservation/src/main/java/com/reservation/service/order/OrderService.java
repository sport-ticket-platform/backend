package com.reservation.service.order;

import com.reservation.common.ApiMessage;
import com.reservation.dto.PageResult;
import com.reservation.dto.order.OrderCancelPenaltyResponse;
import com.reservation.dto.order.OrderDetailResponse;
import com.reservation.dto.order.OrderHistoryRequest;
import com.reservation.handler.BusinessException;
import com.reservation.model.*;
import com.reservation.model.wallet.TransactionReferenceType;
import com.reservation.repository.OrderRepository;
import com.reservation.repository.ReservationRepository;
import com.reservation.service.reservation.ReservationService;
import com.reservation.service.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Provides business logic for order.
 *
 * @author logTAHA
 * @since 1.0.0
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ReservationService reservationService;
    private final WalletService walletService;

    /**
     * Retrieve a paginated list of user's orders, optionally filtered by status.
     */
    public PageResult<Order> getUserOrderHistory(Long userId, OrderHistoryRequest request) {
        int page = request.page();
        int pageSize = request.page_size();
        OrderStatus status = request.status();

        log.info("Fetching order history for userId: {}, page: {}, pageSize: {}, status: {}",
                userId, page, pageSize, status);

        return orderRepository.getUserOrderHistory(userId, page, pageSize, status);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailById(Long orderId, Long userId) {
        log.info("Fetching order details for orderId: {} and userId: {}", orderId, userId);

        Order order = orderRepository.findUserOrderById(orderId, userId)
                .orElseThrow(() -> {
                    log.warn("Order not found or access denied. orderId: {}, userId: {}", orderId, userId);
                    return new BusinessException(ApiMessage.RESOURCE_NOT_FOUND);
                });

        List<SoldSeat> soldSeats = null;
        List<ReservationSeat> reservationSeats = null;
        Long matchId = null;

        if (OrderStatus.PAID.equals(order.getStatus()) || OrderStatus.REFUNDED.equals(order.getStatus())) {
            log.info("Order {} status is {}. Fetching seats from sold_ticket", orderId, order.getStatus().name());
            soldSeats = orderRepository.findUserSoldTicketDetails(orderId);

            if (soldSeats != null && !soldSeats.isEmpty()) {
                matchId = soldSeats.getFirst().getTicketConfig().getMatchId();
            }
        } else {
            log.info("Order {} status is {}. Fetching seats from reservation_seat", orderId, order.getStatus().name());
            reservationSeats = orderRepository.findUserReservationSeatsByOrderId(orderId);

            if (reservationSeats != null && !reservationSeats.isEmpty()) {
                matchId = reservationSeats.getFirst().getTicketConfig().getMatchId();
            }
        }

        return OrderDetailResponse.builder()
                .order(order)
                .matchId(matchId)
                .soldSeats(soldSeats)
                .reservationSeats(reservationSeats)
                .build();
    }

    @Transactional
    public void markOrderAsPaid(Long orderId) {
        orderRepository.updateOrderStatus(orderId, OrderStatus.PAID);
    }

    @Transactional(readOnly = true)
    public OrderCancelPenaltyResponse calculateCancelPenalty(Long orderId, Long userId) {
        log.info("Calculating cancellation penalty for orderId: {} by userId: {}", orderId, userId);

        Order order = orderRepository.findUserOrderById(orderId, userId)
                .orElseThrow(() -> new BusinessException(ApiMessage.ORDER_NOT_FOUND_OR_NOT_YOURS));

        if (order.getStatus() != OrderStatus.PAID) {
            log.warn("Cannot calculate penalty for order {} with status {}", orderId, order.getStatus());
            throw new BusinessException(ApiMessage.ORDER_NOT_PAID_FOR_CALCULATE_CANCELLATION);
        }

        OffsetDateTime matchTime = orderRepository.findMatchTimeByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ApiMessage.RESOURCE_NOT_FOUND));

        long hoursUntilMatch = Duration.between(OffsetDateTime.now(), matchTime).toHours();

        int penaltyPercentage;
        boolean isCancellable = true;

        if (hoursUntilMatch >= 72) {
            penaltyPercentage = 10;
        } else if (hoursUntilMatch >= 24) {
            penaltyPercentage = 30;
        } else if (hoursUntilMatch >= 5) {
            penaltyPercentage = 50;
        } else {
            penaltyPercentage = 100;
            isCancellable = false;
        }

        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal penaltyAmount = totalAmount
                .multiply(BigDecimal.valueOf(penaltyPercentage))
                .divide(BigDecimal.valueOf(100), RoundingMode.CEILING);
        BigDecimal refundableAmount = totalAmount.subtract(penaltyAmount);

        log.info("Calculated penalty is {}({} %). hours until match is: {}", penaltyAmount, penaltyPercentage, hoursUntilMatch);

        return OrderCancelPenaltyResponse.builder()
                .orderId(orderId)
                .totalAmount(totalAmount)
                .penaltyAmount(penaltyAmount)
                .refundableAmount(refundableAmount)
                .penaltyPercentage(penaltyPercentage)
                .isCancellable(isCancellable)
                .build();
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        log.info("User {} requested to cancel order {}", userId, orderId);

        Order order = orderRepository.findUserOrderById(orderId, userId)
                .orElseThrow(() -> new BusinessException(ApiMessage.ORDER_NOT_FOUND_OR_NOT_YOURS));

        switch (order.getStatus()) {
            case FAILED, REFUNDED -> {
                log.warn("Cannot cancel order {} because it is already {}", orderId, order.getStatus());
                throw new BusinessException(ApiMessage.ORDER_CANNOT_BE_CANCELLED);
            }
            case PAID -> {
                log.info("Order {} is PAID. Proceeding to calculate penalty and refund...", orderId);

                OrderCancelPenaltyResponse penaltyInfo = calculateCancelPenalty(orderId, userId);

                if (!penaltyInfo.isCancellable()) {
                    log.warn("Order {} is not cancellable because it is too close to match time.", orderId);
                    throw new BusinessException(ApiMessage.ORDER_CANNOT_BE_CANCELLED);
                }

                orderRepository.updateSoldTicketsStatus(orderId, TicketStatus.CANCELED_BY_USER);

                orderRepository.updateOrderStatus(orderId, OrderStatus.REFUNDED);

                BigDecimal refundableAmount = penaltyInfo.refundableAmount();
                if (refundableAmount.compareTo(BigDecimal.ZERO) > 0) {
                    String description = String.format("Refund for ticket cancellation of order #%d (%d%% penalty applied)",
                            orderId, penaltyInfo.penaltyPercentage());

                    walletService.deposit(
                            userId,
                            refundableAmount,
                            TransactionReferenceType.TICKET_ORDER,
                            orderId,
                            description
                    );
                    log.info("Refunded {} to wallet for user {} against order {}", refundableAmount, userId, orderId);
                }
            }
            case PENDING -> {
                log.info("Order {} is PENDING. Cancelling associated reservation and freeing seats...", orderId);
                reservationService.cancelUserReservation(order.getReservationId(), userId);
            }
        }

        log.info("Cancel operation completed for order {}", orderId);
    }
}


