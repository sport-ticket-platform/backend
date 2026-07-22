package com.backend.dto.user;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String email;

    private String phone;

    @Builder.Default
    private Boolean phoneVerified = false;

    @Builder.Default
    private Boolean emailVerified = false;

    private String firstName;
    private String lastName;
    private String password;
    private UserRole role;

    @Builder.Default
    private boolean isActive = true;

    private String suspendReason;

    @Builder.Default
    private boolean isCredentialExpired = false;

    @Builder.Default
    private boolean isTwoFactorEnabled = false;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdate;
}