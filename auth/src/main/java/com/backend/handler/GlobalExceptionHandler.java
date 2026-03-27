package com.backend.handler;

import com.backend.dto.error.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials() {
        ErrorResponse response = ErrorResponse.builder()
                .status(401)
                .success(false)
                .title("username or password is wrong")
                .message("check your entries and try again")
                .titleFa("نام کاربری یا رمز عبور اشتباه است")
                .messageFa("ورودی های خود را بررسی و دوباره تلاش کنید")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked() {
        ErrorResponse response = ErrorResponse.builder()
                .status(401)
                .success(false)
                .title("Account is locked")
                .message("Try again later")
                .titleFa("اکانت قفل شده است")
                .messageFa("بعدا دوباره تلاش کنید")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401)
                .body(response);
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ErrorResponse> handleCredentialExpired() {
        ErrorResponse response = ErrorResponse.builder()
                .status(401)
                .success(false)
                .title("Your password has been expired")
                .message("use 'password recovery' to reset your password")
                .titleFa("رمز عبور شما منقضی شده است")
                .messageFa("از 'بازیابی رمز عبور' برای ریست کردن پسورد خود استفاده کنید")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401)
                .body(response);
    }

    @ExceptionHandler(UserSuspendException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(UserSuspendException e) {
        ErrorResponse response = ErrorResponse.builder()
                .status(401)
                .success(false)
                .title("Your account is suspend")
                .message(e.getBanReason())
                .titleFa("اکانت شما مسدود شده است")
                .messageFa(e.getBanReason())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401)
                .body(response);
    }
}
