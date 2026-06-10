package com.backend.handler;

import com.backend.common.ApiMessage;
import com.backend.dto.ApiResponse;
import lombok.Builder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Auth errors
    private ResponseEntity<ApiResponse<?>> buildAuthErrorResponse(int status, ApiMessage message) {
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(status)
                .title(message.getTitle())
                .message(message.getMessage())
                .titleFa(message.getTitleFa())
                .messageFa(message.getMessageFa())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials() {
        return buildAuthErrorResponse(
                401,
                ApiMessage.BAD_CREDENTIALS
        );
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<?>> handleLocked() {
        return buildAuthErrorResponse(
                401,
                ApiMessage.ACCOUNT_LOCKED
        );
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ApiResponse<?>> handleCredentialExpired() {
        return buildAuthErrorResponse(
                401,
                ApiMessage.PASSWORD_EXPIRED
        );
    }

    @ExceptionHandler(UserSuspendException.class)
    public ResponseEntity<ApiResponse<?>> handleDisabled(
            UserSuspendException e
    ) {

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(401)
                .title(ApiMessage.ACCOUNT_SUSPENDED.getTitle())
                .message(e.getBanReason())
                .titleFa(ApiMessage.ACCOUNT_SUSPENDED.getTitleFa())
                .messageFa(e.getBanReason())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401)
                .body(response);
    }
}
