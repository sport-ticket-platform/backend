package com.reservation.controller;

import com.reservation.dto.ApiResponse;
import com.reservation.dto.payment.PaymentRequest;
import com.reservation.dto.payment.PaymentResponse;
import com.reservation.service.payment.PaymentService;
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
@RequestMapping("/api/reservations/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<PaymentResponse>> requestPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {

        Long userId = Long.valueOf(authentication.getName());

        log.info("Received payment request for orderId: {} from userId: {}", request.order_id(), userId);

        PaymentResponse response = paymentService.initiatePayment(request.order_id(), userId);

        ApiResponse<PaymentResponse> responseBody = ApiResponse.<PaymentResponse>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Payment request initiated")
                .titleFa("درخواست پرداخت با موفقیت ایجاد شد")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }
}