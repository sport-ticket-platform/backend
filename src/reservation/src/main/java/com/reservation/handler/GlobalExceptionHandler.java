package com.reservation.handler;

import com.reservation.common.ApiMessage;
import com.reservation.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    // ======================================
    //          401 Unauthorized Error
    // ======================================
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());

        ApiMessage msg = ApiMessage.UNAUTHORIZED;

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(401)
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401).body(response);
    }

    // ======================================
    //          403 Forbidden Error
    // ======================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ApiMessage msg = ApiMessage.ACCESS_DENIED;

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(403)
                .title(msg.getTitle())
                .message(msg.getMessage())
                .titleFa(msg.getTitleFa())
                .messageFa(msg.getMessageFa())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(403).body(response);
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
    //         405 Method Not Allowed
    // ======================================
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {

        log.warn("Method not allowed: {}", ex.getMessage());

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(405)
                .title("Method Not Allowed")
                .message("The requested HTTP method is not supported for this endpoint.")
                .titleFa("متد HTTP مجاز نیست")
                .messageFa("متد HTTP ارسال‌شده برای این آدرس API پشتیبانی نمی‌شود.")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(405).body(response);
    }


    // ======================================
    //     400 Invalid Path/Query Variable
    // ======================================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex
    ) {

        log.warn("Method argument type mismatch for parameter '{}': rejected value [{}]",
                ex.getName(), ex.getValue());

        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .title("Invalid Parameter Format")
                .message(String.format("Parameter '%s' expects a valid %s format.", ex.getName(), requiredType))
                .titleFa("فرمت پارامتر نامعتبر است")
                .messageFa(String.format("پارامتر '%s' باید از نوع %s باشد.", ex.getName(), requiredType))
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ======================================
    //           Validation Errors
    // ======================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, List<fieldError>> validationDetails = new HashMap<>();
        ApiMessage validationType = ApiMessage.VALIDATION_FAILED;

        log.warn("Validation failed! Target: {}, Total Errors: {}",
                ex.getBindingResult().getObjectName(),
                ex.getBindingResult().getFieldErrors().size()
        );

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





    // ======================================
    //       Custom Business Exceptions
    // ======================================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(BusinessException ex) {
        ApiMessage apiMessage = ex.getApiMessage();
        Object data = ex.getData();

        log.warn("Business exception occurred: [{}]", apiMessage.name());

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(apiMessage.getStatusCode())
                .title(apiMessage.getTitle())
                .message(apiMessage.getMessage())
                .titleFa(apiMessage.getTitleFa())
                .messageFa(apiMessage.getMessageFa())
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(apiMessage.getStatusCode())
                .body(response);
    }





    // ======================================
    //     Malformed or Missing Request Body
    // ======================================
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {

        log.warn("Malformed or missing request body: {}", ex.getMessage());

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(400) // 400 Bad Request
                .title("Invalid Request Payload")
                .message("Required request body is missing or malformed.")
                .titleFa("درخواست نامعتبر")
                .messageFa("بدنه درخواست (Body) ارسال نشده یا فرمت اطلاعات ارسالی نامعتبر است.")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(400).body(response);
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