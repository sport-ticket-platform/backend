package com.backend.dto.auth.refresh;

import lombok.Builder;

@Builder
public record RefreshResponse(
        String access_token
) {}