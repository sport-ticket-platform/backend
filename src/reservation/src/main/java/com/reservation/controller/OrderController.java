package com.reservation.controller;

import com.reservation.dto.ApiResponse;
import com.reservation.dto.PageResult;
import com.reservation.dto.order.OrderDetailResponse;
import com.reservation.dto.order.OrderHistoryRequest;
import com.reservation.dto.reservation.get.ReservationDetailResponse;
import com.reservation.dto.reservation.get.ReservesHistoryRequest;
import com.reservation.model.Order;
import com.reservation.model.Reservation;
import com.reservation.service.reservation.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/reservations/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResult<Order>>> getOrderHistory(
            @Valid @ModelAttribute OrderHistoryRequest request,
            Authentication authentication
    ) {

        Long userId = Long.valueOf(authentication.getName());

        log.info("Fetch order history for user id: {}, [page: {} | page_size: {} | status: {}]",
                userId, request.page(), request.page_size(), request.status());

        PageResult<Order> historyData = orderService.getUserOrderHistory(userId, request);

        ApiResponse<PageResult<Order>> responseBody = ApiResponse.<PageResult<Order>>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Order history fetched successfully")
                .message(null)
                .titleFa("تاریخچه سفارش ها با موفقیت دریافت شد")
                .messageFa(null)
                .data(historyData)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/{order_id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderById(
            @PathVariable("order_id") Long orderId,
            Authentication authentication
    ) {

        Long userId = Long.valueOf(authentication.getName());

        log.info("Fetch order details for user id: {} and order id: {}", userId, orderId);

        OrderDetailResponse orderData = orderService.getOrderDetailById(orderId, userId);

        ApiResponse<OrderDetailResponse> responseBody = ApiResponse.<OrderDetailResponse>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Order details fetched successfully")
                .message(null)
                .titleFa("جزئیات سفارش با موفقیت دریافت شد")
                .messageFa(null)
                .data(orderData)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }
}