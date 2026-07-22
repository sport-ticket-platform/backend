package com.backend.dto.auth.signup;

import lombok.Builder;

@Builder
public record SignupInitiateResponse(
        String mfa_token
) {
}