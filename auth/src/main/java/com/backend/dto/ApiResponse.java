package com.backend.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApiResponse<T>(
        boolean success,
        int status,
        String title,
        String message,
        String titleFa,
        String messageFa,
        T data,
        LocalDateTime timestamp
) {

}
