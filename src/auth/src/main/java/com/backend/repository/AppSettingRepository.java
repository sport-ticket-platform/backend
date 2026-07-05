package com.backend.repository;

import com.backend.model.AppSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AppSettingRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AppSetting> mapper = (rs, rowNum) -> AppSetting.builder()
            .id(rs.getLong("id"))
            .maxFailedLoginAttempts(rs.getInt("max_failed_login_attempts"))
            .accountLockoutDurationSecond(rs.getInt("account_lockout_duration_second"))
            // اصلاح شده در خط زیر
            .allowConcurrentLogins(rs.getBoolean("allow_concurrent_logins"))
            .maintenanceMode(rs.getBoolean("maintenance_mode"))
            .allowNewRegistrations(rs.getBoolean("allow_new_registrations"))
            .allowLogin(rs.getBoolean("allow_login"))
            .build();

    public Optional<AppSetting> get() {
        String sql = "SELECT * FROM app_setting WHERE id = 1";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, mapper));
        } catch (EmptyResultDataAccessException e) {
            log.warn("Application settings record (ID=1) not found in database.");
            return Optional.empty();
        }
    }

    public void save(AppSetting setting) {
        String sql = """
            INSERT INTO app_setting 
            (id, max_failed_login_attempts, account_lockout_duration_second, 
             allow_concurrent_logins, maintenance_mode, allow_new_registrations, allow_login) 
            VALUES (1, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                max_failed_login_attempts = EXCLUDED.max_failed_login_attempts,
                account_lockout_duration_second = EXCLUDED.account_lockout_duration_second,
                allow_concurrent_logins = EXCLUDED.allow_concurrent_logins,
                maintenance_mode = EXCLUDED.maintenance_mode,
                allow_new_registrations = EXCLUDED.allow_new_registrations,
                allow_login = EXCLUDED.allow_login;
            """;

        jdbcTemplate.update(
                sql,
                setting.getMaxFailedLoginAttempts(),
                setting.getAccountLockoutDurationSecond(),
                setting.getAllowConcurrentLogins(),
                setting.getMaintenanceMode(),
                setting.getAllowNewRegistrations(),
                setting.getAllowLogin()
        );
    }
}