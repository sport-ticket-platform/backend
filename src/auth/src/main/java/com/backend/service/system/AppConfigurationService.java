package com.backend.service.system;

import com.backend.config.ApplicationPolicies;
import com.backend.model.AppSetting;
import com.backend.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppConfigurationService {
    private final ApplicationPolicies appPolicies;
    private final AppSettingRepository settingRepository;
    private final Validator validator;

    @PostConstruct
    protected void loadAppSettingOnStartup() {
        log.info("Loading application settings...");

        AppSetting setting = settingRepository.get()
                .orElseGet(() -> {
                    log.warn("Application settings not found in 'app_setting' table.");
                    log.info("Saving default app settings on database...");
                    AppSetting defaultSetting = AppSetting.builder().build();
                    settingRepository.save(defaultSetting);
                    log.info("default app settings successfully saved on 'app_setting' table.");
                    return defaultSetting;
                });

        validateSetting(setting);
        syncSetting(setting);
        log.info("Application settings loaded successfully.");
    }

    public void updateApplicationSettings(AppSetting newSettings) {
        log.info("Start updating application settings...");
        newSettings.setId(1L);

        validateSetting(newSettings);

        settingRepository.save(newSettings);

        syncSetting(newSettings);
        log.info("Application settings update successfully.");
    }

    private void syncSetting(AppSetting setting) {
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