package com.backend.dto.auth.refresh;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "REFRESH_TOKEN_REQUIRED")
        @Size(min = 35, max = 37, message = "REFRESH_TOKEN_INVALID_SIZE")
        String refresh_token
) {
}