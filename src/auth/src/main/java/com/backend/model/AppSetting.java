package com.backend.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting {

    @Builder.Default
    private Long id = 1L;

    /**
     * Dynamic Security Settings
     */
    @NotNull
    @Min(1)
    @Builder.Default
    private Integer maxFailedLoginAttempts = 5;

    @NotNull
    @Min(60)
    @Builder.Default
    private Integer accountLockoutDurationSecond = 600;

    @NotNull
    @Builder.Default
    private Boolean allowConcurrentLogins = true;

    /**
     * General Settings
     */
    @NotNull
    @Builder.Default
    private Boolean maintenanceMode = false;

    @NotNull
    @Builder.Default
    private Boolean allowNewRegistrations = true;

    @NotNull
    @Builder.Default
    private Boolean allowLogin = true;
}