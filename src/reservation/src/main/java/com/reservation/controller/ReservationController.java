package com.reservation.controller;

import com.reservation.dto.ApiResponse;
import com.reservation.dto.reservation.ReservationRequest;
import com.reservation.dto.reservation.ReservationResponse;
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
}