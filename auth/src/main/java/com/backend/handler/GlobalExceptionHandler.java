package com.backend.handler;

import com.backend.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials() {
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(401)
                .title("username or password is wrong")
                .message("check your entries and try again")
                .titleFa("نام کاربری یا رمز عبور اشتباه است")
                .messageFa("ورودی های خود را بررسی و دوباره تلاش کنید")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<?>> handleLocked() {
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(401)
                .title("Account is locked")
                .message("Try again later")
                .titleFa("اکانت قفل شده است")
                .messageFa("بعدا دوباره تلاش کنید")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401)
                .body(response);
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ApiResponse<?>> handleCredentialExpired() {
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(401)
                .title("Your password has been expired")
                .message("use 'password recovery' to reset your password")
                .titleFa("رمز عبور شما منقضی شده است")
                .messageFa("از 'بازیابی رمز عبور' برای ریست کردن پسورد خود استفاده کنید")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401)
                .body(response);
    }

    @ExceptionHandler(UserSuspendException.class)
    public ResponseEntity<ApiResponse<?>> handleDisabled(UserSuspendException e) {
        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .status(401)
                .title("Your account is suspend")
                .message(e.getBanReason())
                .titleFa("اکانت شما مسدود شده است")
                .messageFa(e.getBanReason())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401)
                .body(response);
    }
}
