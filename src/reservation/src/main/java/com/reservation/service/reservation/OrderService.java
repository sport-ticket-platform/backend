package com.reservation.service.reservation;

import com.reservation.common.ApiMessage;
import com.reservation.dto.PageResult;
import com.reservation.dto.order.OrderDetailResponse;
import com.reservation.dto.order.OrderHistoryRequest;
import com.reservation.handler.BusinessException;
import com.reservation.model.*;
import com.reservation.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}


