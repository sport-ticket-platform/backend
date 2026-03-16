package com.backend.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@Component
public class ApplicationPolicies {
    /** Dynamic Security Settings */
    private volatile Boolean captchaEnabled;
    private volatile Integer maxFailedLoginAttempts;
    private volatile Integer accountLockoutDurationSecond;
    private volatile Boolean allowConcurrentLogins;
    /** General Settings */
    private volatile Boolean maintenanceMode;
    private volatile Boolean allowNewRegistrations;
    private volatile Boolean allowLogin;
}
