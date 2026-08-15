package com.reservation.controller;

import com.reservation.dto.ApiResponse;
import com.reservation.dto.PageResult;
import com.reservation.dto.payment.PaymentCallbackRequest;
import com.reservation.dto.payment.PaymentCallbackResponse;
import com.reservation.dto.payment.PaymentRequest;
import com.reservation.dto.payment.PaymentResponse;
import com.reservation.dto.payment.get.PaymentHistoryRequest;
import com.reservation.dto.payment.get.PaymentHistoryResponse;
import com.reservation.dto.wallet.WalletPaymentRequest;
import com.reservation.dto.wallet.WalletPaymentResponse;
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

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<PaymentCallbackResponse>> processPaymentCallback(
            @Valid @RequestBody PaymentCallbackRequest request,
            Authentication authentication) {

        Long userId = Long.valueOf(authentication.getName());

        log.info("Processing payment callback for token: {} with status: {} by userId: {}",
                request.token(), request.status(), userId);

        PaymentCallbackResponse response = paymentService.processCallback(request, userId);

        ApiResponse<PaymentCallbackResponse> responseBody = ApiResponse.<PaymentCallbackResponse>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Payment callback processed")
                .titleFa("نتيجه پرداخت با موفقیت ثبت شد")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/pay-with-wallet")
    public ResponseEntity<ApiResponse<WalletPaymentResponse>> payWithWallet(
            @Valid @RequestBody WalletPaymentRequest request,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        log.info("Received wallet payment request for orderId: {} by userId: {}", request.order_id(), userId);

        WalletPaymentResponse response = paymentService.payWithWallet(request.order_id(), userId);

        ApiResponse<WalletPaymentResponse> responseBody = ApiResponse.<WalletPaymentResponse>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Paid successfully with wallet")
                .titleFa("پرداخت با کیف پول با موفقیت انجام شد")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResult<PaymentHistoryResponse>>> getPaymentHistory(
            @Valid @ModelAttribute PaymentHistoryRequest request,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        log.info("Fetch payment history for user id: {}, [page: {} | page_size: {} | status: {}]",
                userId, request.page(), request.page_size(), request.status());

        PageResult<PaymentHistoryResponse> historyData = paymentService.getUserPaymentHistory(userId, request);

        ApiResponse<PageResult<PaymentHistoryResponse>> responseBody = ApiResponse.<PageResult<PaymentHistoryResponse>>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Payment history fetched successfully")
                .message(null)
                .titleFa("تاریخچه پرداخت‌ها با موفقیت دریافت شد")
                .messageFa(null)
                .data(historyData)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }
}