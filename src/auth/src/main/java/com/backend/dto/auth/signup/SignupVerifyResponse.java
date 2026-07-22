package com.backend.dto.auth.signup;

import lombok.Builder;

@Builder
public record SignupVerifyResponse(
        String temp_token
) {
}