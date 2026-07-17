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
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // record for handleValidationErrors
    private record fieldError(String message, String messageFa) {}

    private static final Map<String, Integer> ERROR_PRIORITY = Map.of(
            "NotBlank", 1,
            "Size", 2,
            "Pattern", 3
    );

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

    @ExceptionHandler(CustomLockedException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomLocked(CustomLockedException e) {
        ApiMessage apiMessage = e.getApiMessage();

        StringBuilder timeEn = new StringBuilder();
        StringBuilder timeFa = new StringBuilder();

        if (e.getHours() > 0) {
            timeEn.append(e.getHours()).append(" hours, ");
            timeFa.append(e.getHours()).append(" ساعت و ");
        }
        if (e.getMinutes() > 0 || e.getHours() > 0) {
            timeEn.append(e.getMinutes()).append(" minutes, ");
            timeFa.append(e.getMinutes()).append(" دقیقه و ");
        }
        timeEn.append(e.getSeconds()).append(" seconds");
        timeFa.append(e.getSeconds()).append(" ثانیه");

        String customMessageEn = apiMessage.getMessage() + " Please try again in " + timeEn.toString() + ".";
        String customMessageFa = apiMessage.getMessageFa() + " " + timeFa.toString() + " دیگر تلاش کنید.";

        Map<String, Long> timeData = Map.of(
                "hours", e.getHours(),
                "minutes", e.getMinutes(),
                "seconds", e.getSeconds()
        );

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(apiMessage.getStatusCode())
                .title(apiMessage.getTitle())
                .message(customMessageEn)
                .titleFa(apiMessage.getTitleFa())
                .messageFa(customMessageFa)
                .data(timeData)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(apiMessage.getStatusCode()).body(response);
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


    // ============================
    //      Validation Errors
    // ============================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, List<fieldError>> validationDetails = new HashMap<>();

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

        ex.getBindingResult().getFieldErrors().stream()
                .sorted(java.util.Comparator.comparingInt(
                        fe -> ERROR_PRIORITY.getOrDefault(fe.getCode(), 99))
                )
                .forEach(fieldError -> {
                    String fieldName = fieldError.getField();
                    String messageKey = fieldError.getDefaultMessage();

                    ApiMessage msg;
                    try {
                        msg = ApiMessage.valueOf(messageKey);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        log.error(
                                "Missing Enum constant for validation message key: [{}] on field: [{}]",
                                messageKey, fieldName
                        );
                        msg = ApiMessage.VALIDATION_FAILED;
                    }

                    log.debug("Field [{}] validation failed. Key: [{}], Code: [{}], Rejected Value: [{}]",
                            fieldName, messageKey, fieldError.getCode(), fieldError.getRejectedValue()
                    );

                    fieldError validError = new fieldError(msg.getMessage(), msg.getMessageFa());

                    validationDetails.computeIfAbsent(
                            fieldName, key -> new java.util.ArrayList<>()).add(validError
                    );
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


    // =====================
    //       Auth error
    // =====================
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthException(AuthException ex) {
        ApiMessage msg = ex.getApiMessage();

        Map<String, List<fieldError>> errorData = null;

        if (ex.getFieldName() != null) {
            errorData = new HashMap<>();
            fieldError fieldError = new fieldError(msg.getMessage(), msg.getMessageFa());
            errorData.computeIfAbsent(ex.getFieldName(), k -> new ArrayList<>()).add(fieldError);
        }

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(errorData)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(msg.getStatusCode()).body(response);
    }

    // ======================================
    //          404 Not Found Error
    // ======================================
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(NoResourceFoundException ex) {

        log.warn("Resource not found: /{}", ex.getResourcePath());

        ApiMessage msg = ApiMessage.RESOURCE_NOT_FOUND;

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(msg.getStatusCode())
                .title(msg.getTitle())
                .message(msg.getMessage() + ": /" + ex.getResourcePath())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa() + ": /" + ex.getResourcePath())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(msg.getStatusCode()).body(response);
    }

    // ======================================
    //     Global 500 Internal Server Error
    // ======================================
    @ExceptionHandler(org.springframework.security.authentication.AuthenticationServiceException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationServiceException(
            org.springframework.security.authentication.AuthenticationServiceException ex) {

        log.error("Authentication infrastructure error: {}", ex.getMessage(), ex);
        return buildAuthErrorResponse(ApiMessage.AUTH_SERVICE_UNAVAILABLE);
    }

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
