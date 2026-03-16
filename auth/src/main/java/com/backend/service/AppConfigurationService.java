package com.backend.service;

import com.backend.config.ApplicationPolicies;

import com.backend.entity.AppSetting;
import com.backend.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppConfigurationService {
    private final ApplicationPolicies appPolicies;
    private final AppSettingRepository settingRepository;
    private final Validator validator;

    @PostConstruct
    @Transactional
    protected void loadAppSettingOnStartup() {
        log.info("Loading application settings...");

        AppSetting setting = settingRepository.findById(1L)
                .orElseGet(() -> {
                    log.warn("Application settings not found in 'application_setting' table.");
                    log.info("Saving default app settings on database...");
                    AppSetting defaultSetting = settingRepository.save(new AppSetting());
                    log.info("default app settings successfully saved on 'application_setting' table.");
                    return defaultSetting;
                });

        validateSetting(setting);

        syncSetting(setting);
        log.info("Application settings loaded successfully.");
    }

    @Transactional
    public void updateApplicationSettings(AppSetting newSettings) {
        log.info("Start updating application settings...");
        newSettings.setId(1L);

        validateSetting(newSettings);

        AppSetting savedSetting = settingRepository.save(newSettings);
        syncSetting(savedSetting);
        log.info("Application settings update successfully.");
    }

    private void syncSetting(AppSetting setting) {
        appPolicies.setCaptchaEnabled(setting.getCaptchaEnabled());
        appPolicies.setMaxFailedLoginAttempts(setting.getMaxFailedLoginAttempts());
        appPolicies.setAccountLockoutDurationSecond(setting.getAccountLockoutDurationSecond());
        appPolicies.setAllowConcurrentLogins(setting.getAllowConcurrentLogins());
        appPolicies.setMaintenanceMode(setting.getMaintenanceMode());
        appPolicies.setAllowNewRegistrations(setting.getAllowNewRegistrations());
        appPolicies.setAllowLogin(setting.getAllowLogin());
    }

    private void validateSetting(AppSetting setting) {
        var violations = validator.validate(setting);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            log.error("Application settings are invalid in database.");
            throw new IllegalStateException("Invalid application setting in DB: " + message);
        }
    }
}