package com.backend.dto.error;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse(
        int status,
        boolean success,
        String title,
        String message,
        LocalDateTime timestamp,
        // Persian
        String titleFa,
        String messageFa
) {
}
