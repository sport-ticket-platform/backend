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
    private ResponseEntity<ApiResponse<?>> buildAuthErrorResponse(ApiMessage message) {
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(message.getStatusCode())
                .title(message.getTitle())
                .message(message.getMessage())
                .titleFa(message.getTitleFa())
                .messageFa(message.getMessageFa())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(message.getStatusCode()).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials() {
        return buildAuthErrorResponse(ApiMessage.BAD_CREDENTIALS);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<?>> handleLocked() {
        return buildAuthErrorResponse(ApiMessage.ACCOUNT_LOCKED);
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ApiResponse<?>> handleCredentialExpired() {
        return buildAuthErrorResponse(ApiMessage.PASSWORD_EXPIRED);
    }

    @ExceptionHandler(UserSuspendException.class)
    public ResponseEntity<ApiResponse<?>> handleDisabled(
            UserSuspendException e
    ) {

        ApiMessage msg = ApiMessage.ACCOUNT_SUSPENDED;
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(e.getBanReason())
                .titleFa(msg.getTitleFa())
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
                .status(validationType.getStatusCode())
                .title(validationType.getTitle())
                .message(validationType.getMessage())
                .titleFa(validationType.getTitleFa())
                .messageFa(validationType.getMessageFa())
                .data(validationDetails)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(validationType.getStatusCode()).body(response);
    }

    // Rate Limit Handler
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiResponse<?>> handleRateLimit() {
        ApiMessage msg = ApiMessage.TOO_MANY_REQUESTS;
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(msg.getStatusCode()).body(response);
    }

    // Auth error
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthException(AuthException ex) {
        ApiMessage msg = ex.getApiMessage();

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(ex.getFieldName() != null ? Map.of("field", ex.getFieldName()) :  null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(msg.getStatusCode()).body(response);
    }






    // ======================================
    //     Global 500 Internal Server Error
    // ======================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception ex) {

        log.error("Unhandled Exception Caught: ", ex);

        ApiMessage msg = ApiMessage.INTERNAL_SERVER_ERROR;
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(msg.getStatusCode()).body(response);
    }
}
