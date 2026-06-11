package com.backend.dto.auth;

import lombok.Builder;

@Builder
public record SignupResponse (
        Long userId
        // String accessToken,
        // String refreshToken
) {
}