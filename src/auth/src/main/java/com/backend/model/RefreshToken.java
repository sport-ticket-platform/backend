package com.backend.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    private Long id;
    private String token;

    private Long userId;

    private LocalDateTime createdAt;
    private LocalDateTime expirationDate;

    @Builder.Default
    private boolean isActive = true;

    private LocalDateTime revokedAt;
    private String revokedReason;
    private String ipAddress;
    private String userAgent;
    private String deviceId;
}