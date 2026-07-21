package com.backend.controller;

import com.backend.annotation.docs.SignupApiDocs;
import com.backend.common.ApiMessage;
import com.backend.config.ApplicationProperties;
import com.backend.dto.ApiResponse;
import com.backend.dto.auth.login.*;
import com.backend.dto.auth.refresh.RefreshRequest;
import com.backend.dto.auth.refresh.RefreshResponse;
import com.backend.dto.auth.signup.SignupRequest;
import com.backend.dto.auth.signup.SignupResponse;
import com.backend.service.auth.AuthService;
import com.backend.service.auth.RefreshTokenService;
import com.backend.service.system.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * <h2>Authentication Controller</h2>
 * <p>
 * REST controller responsible for handling authentication flows.
 * Exposes endpoints for user loginWithPassword and token generation.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authSrv;
    private final RefreshTokenService refreshTokenSrv;
    private final RateLimitService rateLimitSrv;

    private final ApplicationProperties appPrp;

    // ===================
    //       Login
    // ===================
    @PostMapping("/login-password")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithPassword(
            @Valid @RequestBody LoginWithPassRequest loginWithPassRequest,
            HttpServletRequest request
    ) {

        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String deviceId = request.getHeader("X-Device-Id");

        LoginResponse response = authSrv.loginWithPassword(loginWithPassRequest, ipAddress, userAgent, deviceId);

        // Default is send notif with email
        ApiMessage msg = switch (response.step()) {
            case "2FA-EMAIL" -> ApiMessage.LOGIN_SUCCESS_NEED_2FA_EMAIL;
            case "2FA-PHONE" -> ApiMessage.LOGIN_SUCCESS_NEED_2FA_PHONE;
            default -> ApiMessage.LOGIN_SUCCESS;
        };

        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .status(200)
                        .title(msg.getTitle())
                        .message(msg.getMessage())
                        .titleFa(msg.getTitleFa())
                        .messageFa(msg.getMessageFa())
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/login-otp-email")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithOTPEmail(
            @Valid @RequestBody LoginOTPEmailRequest loginOTPEmailRequest
    ) {

        LoginResponse response = authSrv.loginWithOTPEmail(loginOTPEmailRequest);


        ApiMessage msg = ApiMessage.LOGIN_EMAIL_OTP_SENT;

        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .status(200)
                        .title(msg.getTitle())
                        .message(msg.getMessage())
                        .titleFa(msg.getTitleFa())
                        .messageFa(msg.getMessageFa())
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/login-otp-phone")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithOTPPhone(
            @Valid @RequestBody LoginOTPPhoneRequest loginOTPPhoneRequest
            ) {

        LoginResponse response = authSrv.loginWithOTPPhone(loginOTPPhoneRequest);


        ApiMessage msg = ApiMessage.LOGIN_PHONE_OTP_SENT;

        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .status(200)
                        .title(msg.getTitle())
                        .message(msg.getMessage())
                        .titleFa(msg.getTitleFa())
                        .messageFa(msg.getMessageFa())
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verify(
            @Valid @RequestBody VerifyRequest verifyRequest,
            HttpServletRequest request
    ) {
        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String deviceId = request.getHeader("X-Device-Id");

        LoginResponse response = authSrv.verifyOTP(verifyRequest, ipAddress, userAgent, deviceId);


        ApiMessage msg = switch (response.step()) {
            case "2FA-EMAIL" -> ApiMessage.LOGIN_SUCCESS_NEED_2FA_EMAIL;
            case "2FA-PHONE" -> ApiMessage.LOGIN_SUCCESS_NEED_2FA_PHONE;
            default -> ApiMessage.LOGIN_SUCCESS;
        };

        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .status(200)
                        .title(msg.getTitle())
                        .message(msg.getMessage())
                        .titleFa(msg.getTitleFa())
                        .messageFa(msg.getMessageFa())
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


    // ===================
    //       Signup
    // ===================
    @SignupApiDocs
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest,
            HttpServletRequest request
    ) {

        String ipAddress = extractClientIp(request);

        log.info("Signing up for email: [{}] and IP: [{}]", signupRequest.email(), ipAddress);

        SignupResponse responseData = authSrv.signup(signupRequest);

        ApiMessage msg = ApiMessage.SIGNUP_SUCCESS;
        ApiResponse<SignupResponse> response = ApiResponse.<SignupResponse>builder()
                .success(true)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(responseData)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // ===================================
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @Valid @RequestBody RefreshRequest refreshRequest
    ) {

        RefreshResponse response = refreshTokenSrv.refresh(refreshRequest.refresh_token());
        ApiMessage msg = ApiMessage.REFRESH_SUCCESS;
        return ResponseEntity.ok(
                ApiResponse.<RefreshResponse>builder()
                        .success(true)
                        .status(200)
                        .title(msg.getTitle())
                        .message(msg.getMessage())
                        .titleFa(msg.getTitleFa())
                        .messageFa(msg.getMessageFa())
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
