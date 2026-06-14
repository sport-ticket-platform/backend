package com.backend.dto.auth;

import lombok.Builder;

@Builder
public record SignupResponse (
        Long user_id
        // String accessToken,
        // String refreshToken
) {
}