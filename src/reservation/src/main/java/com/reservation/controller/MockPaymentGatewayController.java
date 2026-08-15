package com.reservation.controller;

import com.reservation.dto.ApiResponse;
import com.reservation.dto.payment.mockgateway.MockPaymentInfoResponse;
import com.reservation.service.mockgateway.MockPaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/reservations/mock-gateway")
@RequiredArgsConstructor
public class MockPaymentGatewayController {

    private final MockPaymentGatewayService mockPaymentGatewayService;

    @GetMapping("/info/{token}")
    public ResponseEntity<ApiResponse<MockPaymentInfoResponse>> getMockPaymentInfo(
            @PathVariable("token") String token) {

        log.info("Received request to get mock payment info for token: {}", token);

        MockPaymentInfoResponse response = mockPaymentGatewayService.getMockPaymentInfo(token);

        ApiResponse<MockPaymentInfoResponse> responseBody = ApiResponse.<MockPaymentInfoResponse>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Payment info fetched successfully")
                .titleFa("اطلاعات پرداخت با موفقیت دریافت شد")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }
}