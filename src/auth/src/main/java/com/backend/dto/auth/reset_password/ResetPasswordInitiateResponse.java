package com.backend.dto.auth.reset_password;

import lombok.Builder;

@Builder
public record ResetPasswordInitiateResponse(
        String mfa_token
) {
}