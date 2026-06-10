package com.backend.handler;

import com.backend.common.ApiMessage;
import com.backend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // record for handleValidationErrors
    private record ValidError(String message, String messageFa) {}

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

    // Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, java.util.List<ValidError>> validationDetails = new HashMap<>();

        String firstMessageKey = ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();

        ApiMessage validationType = ApiMessage.VALIDATION_FAILED;

        log.warn("Validation failed! Target: {}, Total Errors: {}",
                ex.getBindingResult().getObjectName(),
                ex.getBindingResult().getFieldErrors().size()
        );

        if (firstMessageKey != null) {
            if (firstMessageKey.startsWith("LOGIN_"))
                validationType = ApiMessage.LOGIN_VALIDATION_FAILED;
            else if (firstMessageKey.startsWith("SIGNUP_"))
                validationType = ApiMessage.SIGNUP_VALIDATION_FAILED;
        }

        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String messageKey = fieldError.getDefaultMessage();

            ApiMessage msg;
            try {
                msg = ApiMessage.valueOf(messageKey);
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("Missing Enum constant for validation message key: [{}] on field: [{}]", messageKey, fieldName);
                msg = ApiMessage.VALIDATION_FAILED;
            }

            log.debug("Field [{}] validation failed. Key: [{}], Rejected Value: [{}]",
                    fieldName, messageKey, fieldError.getRejectedValue()
            );

            ValidError validError = new ValidError(msg.getMessage(), msg.getMessageFa());

            validationDetails.computeIfAbsent(fieldName, key -> new java.util.ArrayList<>()).add(validError);
        });

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(400)
                .title(validationType.getTitle())
                .message(validationType.getMessage())
                .titleFa(validationType.getTitleFa())
                .messageFa(validationType.getMessageFa())
                .data(validationDetails)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(400).body(response);
    }
}
