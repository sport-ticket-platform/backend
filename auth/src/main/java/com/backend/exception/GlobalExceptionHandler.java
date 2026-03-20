package com.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials() {
        return ResponseEntity.status(401)
                .body("نام کاربری یا رمز عبور اشتباه است");
    }
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<?> handleLocked() {
        return ResponseEntity.status(423)
                .body("اکانت قفل شده است");
    }

}
