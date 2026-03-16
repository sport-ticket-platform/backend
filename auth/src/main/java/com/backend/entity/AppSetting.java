package com.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor

@Entity
@Table(name = "application_setting")
public class AppSetting {

    @Id
    @Column(updatable = false, nullable = false)
    private Long id = 1L;

    /**
     * Dynamic Security Settings
     */
    @NotNull
    @Column(nullable = false)
    private Boolean captchaEnabled = true;

    @NotNull @Min(1)
    @Column(nullable = false)
    private Integer maxFailedLoginAttempts = 5;

    @NotNull @Min(60)
    @Column(nullable = false)
    private Integer accountLockoutDurationSecond = 600;

    @NotNull
    @Column(nullable = false)
    private Boolean allowConcurrentLogins = true;

    /**
     * General Settings
     */
    @NotNull
    @Column(nullable = false)
    private Boolean maintenanceMode = false;

    @NotNull
    @Column(nullable = false)
    private Boolean allowNewRegistrations = true;

    @NotNull
    @Column(nullable = false)
    private Boolean allowLogin = true;

    @PrePersist
    protected void prePersist() {
        if (id == null || id != 1) {
            throw new IllegalStateException("The 'application_setting' table, should have just one record");
        }
    }
}
