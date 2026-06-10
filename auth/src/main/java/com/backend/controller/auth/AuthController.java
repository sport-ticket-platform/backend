package com.backend.controller.auth;

import com.backend.common.ApiMessage;
import com.backend.config.ApplicationProperties;
import com.backend.dto.ApiResponse;
import com.backend.dto.auth.*;
import com.backend.handler.RateLimitException;
import com.backend.service.auth.AuthService;
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
 * Exposes endpoints for user login and token generation.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authSrv;
    private final RateLimitService rateLimitSrv;

    private final ApplicationProperties appPrp;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {

        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String deviceId = request.getHeader("X-Device-Id");

        LoginResponse response = authSrv.login(loginRequest, ipAddress, userAgent, deviceId);


        ApiMessage msg = ApiMessage.SUCCESS_LOGIN;
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

    // Signup
    @PostMapping("/check-username")
    public ResponseEntity<ApiResponse<?>> checkUsername(
            @Valid @RequestBody CheckUsernameRequest checkRequest,
            HttpServletRequest request
    ) {

        String ipAddress = extractClientIp(request);
        if (!rateLimitSrv.isIpAllowed(
                ipAddress, "check-username",
                appPrp.getEndpointLimitsPerMin().getCheckUsername(),
                60)
        ) {
            throw new RateLimitException("This IP sent too many requests");
        }

        log.info("Checking username: [{}] uniqueness for IP: {}", checkRequest.username(), ipAddress);
        CheckUsernameResponse responseData = authSrv.checkUsernameUnique(checkRequest.username());

        ApiResponse<CheckUsernameResponse> response = ApiResponse.<CheckUsernameResponse>builder()
                .success(true)
                .status(200)
                .title(null)
                .message(null)
                .titleFa(null)
                .messageFa(null)
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
}
