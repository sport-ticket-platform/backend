package com.reservation.controller;

import com.reservation.dto.ApiResponse;
import com.reservation.dto.PageResult;
import com.reservation.dto.reservation.ReservationRequest;
import com.reservation.dto.reservation.ReservationResponse;
import com.reservation.dto.reservation.get.ReservationDetailResponse;
import com.reservation.dto.reservation.get.ReservesHistoryRequest;
import com.reservation.model.Reservation;
import com.reservation.service.reservation.ReservationService;
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
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationSer;

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveSeats(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication
    ) {

        Long userId = Long.valueOf(authentication.getName());

        log.info("Reservation request for user id: {}", userId);

        ReservationResponse reservationData = reservationSer.reserve(userId, request);

        ApiResponse<ReservationResponse> responseBody = ApiResponse.<ReservationResponse>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Reservation submitted successfully")
                .message(null)
                .titleFa("رزرو با موفقیت انجام شد")
                .messageFa(null)
                .data(reservationData)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/reserve/history")
    public ResponseEntity<ApiResponse<PageResult<Reservation>>> getReservationHistory(
            @Valid @ModelAttribute ReservesHistoryRequest request,
            Authentication authentication
    ) {

        Long userId = Long.valueOf(authentication.getName());

        log.info("Fetch reservation history for user id: {}, [page: {} | page_size: {} | status: {}]",
                userId, request.page(), request.page_size(), request.status());

        PageResult<Reservation> historyData = reservationSer.getUserReservationHistory(userId, request);

        ApiResponse<PageResult<Reservation>> responseBody = ApiResponse.<PageResult<Reservation>>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Reservation history fetched successfully")
                .message(null)
                .titleFa("تاریخچه رزروها با موفقیت دریافت شد")
                .messageFa(null)
                .data(historyData)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/reserve/{reservation_id}")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationById(
            @PathVariable("reservation_id") Long reservationId,
            Authentication authentication
    ) {

        Long userId = Long.valueOf(authentication.getName());

        log.info("Fetch reservation details for user id: {} and reservation id: {}", userId, reservationId);

        ReservationDetailResponse reservationData = reservationSer.getReservationById(userId, reservationId);

        ApiResponse<ReservationDetailResponse> responseBody = ApiResponse.<ReservationDetailResponse>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .title("Reservation details fetched successfully")
                .message(null)
                .titleFa("جزئیات رزرو با موفقیت دریافت شد")
                .messageFa(null)
                .data(reservationData)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(responseBody);
    }
}