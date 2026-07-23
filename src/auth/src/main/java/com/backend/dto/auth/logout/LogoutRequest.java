package com.backend.dto.auth.logout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(
        @NotBlank(message = "REFRESH_TOKEN_REQUIRED")
        @Size(min = 36, max = 36, message = "REFRESH_TOKEN_INVALID_SIZE")
        String refresh_token
) {}