package com.reservation.controller;

import com.reservation.dto.ApiResponse;
import com.reservation.dto.wallet.get.WalletInfoRequest;
import com.reservation.dto.wallet.get.WalletInfoResponse;
import com.reservation.service.wallet.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/reservations/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<WalletInfoResponse>> getWalletInfoAndHistory(
            @Valid @ModelAttribute WalletInfoRequest request,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        log.info("Fetching wallet info and transactions for userId: {}, [page: {} | page_size: {} | type: {} | status: {}]",
                userId, request.page(), request.page_size(), request.type(), request.status());

        WalletInfoResponse walletInfo = walletService.getWalletInfoWithHistory(userId, request);

        ApiResponse<WalletInfoResponse> responseBody = ApiResponse.<WalletInfoResponse>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Wallet information fetched successfully")
                .message(null)
                .titleFa("اطلاعات کیف پول با موفقیت دریافت شد")
                .messageFa(null)
                .data(walletInfo)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }
}