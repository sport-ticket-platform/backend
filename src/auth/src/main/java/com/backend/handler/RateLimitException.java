package com.backend.handler;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}