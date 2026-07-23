package com.backend.controller;

import com.backend.common.ApiMessage;
import com.backend.dto.ApiResponse;
import com.backend.dto.auth.VerifyRequest;
import com.backend.dto.auth.login.*;
import com.backend.dto.auth.logout.LogoutRequest;
import com.backend.dto.auth.refresh.RefreshRequest;
import com.backend.dto.auth.refresh.RefreshResponse;
import com.backend.dto.auth.reset_password.ResetPasswordCompleteRequest;
import com.backend.dto.auth.reset_password.ResetPasswordInitiateRequest;
import com.backend.dto.auth.reset_password.ResetPasswordInitiateResponse;
import com.backend.dto.auth.reset_password.ResetPasswordVerifyResponse;
import com.backend.dto.auth.signup.SignupCompleteRequest;
import com.backend.dto.auth.signup.SignupInitiateRequest;
import com.backend.dto.auth.signup.SignupInitiateResponse;
import com.backend.dto.auth.signup.SignupVerifyResponse;
import com.backend.service.auth.AuthService;
import com.backend.service.auth.RefreshTokenService;
import com.backend.service.auth.SignupService;
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
 * Exposes endpoints for user login and token generation.
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
    private final SignupService signupSrv;
    private final RefreshTokenService refreshTokenSrv;

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

        LoginResponse response = authSrv.verifyLoginOTP(verifyRequest, ipAddress, userAgent, deviceId);


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
    @PostMapping("/signup/initiate")
    public ResponseEntity<ApiResponse<SignupInitiateResponse>> signupInitiate(
            @Valid @RequestBody SignupInitiateRequest signupInitiateRequest,
            HttpServletRequest request
    ) {

        String ipAddress = extractClientIp(request);

        log.info("Signing up for email: [{}] and IP: [{}]", signupInitiateRequest.email(), ipAddress);

        SignupInitiateResponse responseData = signupSrv.signupInitiate(signupInitiateRequest);

        ApiMessage msg = ApiMessage.SIGNUP_INITIATE_EMAIL_SUCCESS;
        ApiResponse<SignupInitiateResponse> response = ApiResponse.<SignupInitiateResponse>builder()
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

    @PostMapping("/signup/verify")
    public ResponseEntity<ApiResponse<SignupVerifyResponse>> signupVerify(
            @Valid @RequestBody VerifyRequest request
    ) {

        log.info("Received signup OTP verification request for MFA token: [{}]", request.mfa());

        SignupVerifyResponse responseData = signupSrv.verifySignupOTP(request);

        ApiMessage msg = ApiMessage.SIGNUP_VERIFY_OTP_SUCCESS;
        ApiResponse<SignupVerifyResponse> response = ApiResponse.<SignupVerifyResponse>builder()
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

    @PostMapping("/signup/complete")
    public ResponseEntity<ApiResponse<Void>> signupComplete(
            @Valid @RequestBody SignupCompleteRequest request
    ) {
        log.info("Received request to complete registration for temp token: [{}]", request.temp_token());

        signupSrv.signupComplete(request);

        ApiMessage msg = ApiMessage.SIGNUP_SUCCESS;

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    // ==============================================
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


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        log.info("Received logout request");

        if (request.refresh_token() != null && !request.refresh_token().isBlank()) {
            refreshTokenSrv.revokeRefreshToken(request.refresh_token());
        }

        ApiMessage msg = ApiMessage.LOGOUT_SUCCESS;

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password/initiate")
    public ResponseEntity<ApiResponse<ResetPasswordInitiateResponse>> resetPasswordInitiate(
            @Valid @RequestBody ResetPasswordInitiateRequest request,
            HttpServletRequest httpRequest
    ) {

        String ipAddress = extractClientIp(httpRequest);

        log.info("Initiating password reset for email: [{}] and IP: [{}]", request.email(), ipAddress);

        ResetPasswordInitiateResponse responseData = authSrv.initiatePasswordReset(request);

        ApiMessage msg = ApiMessage.RESET_PASSWORD_INITIATE_EMAIL_SUCCESS;

        ApiResponse<ResetPasswordInitiateResponse> response = ApiResponse.<ResetPasswordInitiateResponse>builder()
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

    @PostMapping("/reset-password/verify")
    public ResponseEntity<ApiResponse<ResetPasswordVerifyResponse>> resetPasswordVerify(
            @Valid @RequestBody VerifyRequest request
    ) {

        log.info("Received password reset OTP verification request for MFA token: [{}]", request.mfa());

        ResetPasswordVerifyResponse responseData = authSrv.verifyResetPasswordOTP(request);

        ApiMessage msg = ApiMessage.RESET_PASSWORD_VERIFY_OTP_SUCCESS;

        ApiResponse<ResetPasswordVerifyResponse> response = ApiResponse.<ResetPasswordVerifyResponse>builder()
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

    @PostMapping("/reset-password/complete")
    public ResponseEntity<ApiResponse<Void>> resetPasswordComplete(
            @Valid @RequestBody ResetPasswordCompleteRequest request
    ) {
        log.info("Received request to complete password reset for temp token: [{}]", request.temp_token());

        authSrv.completePasswordReset(request);

        ApiMessage msg = ApiMessage.RESET_PASSWORD_SUCCESS;

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(null)
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
}
