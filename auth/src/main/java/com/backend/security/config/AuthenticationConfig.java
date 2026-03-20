package com.backend.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <h2>Configure Authentication & Authorization</h2>
 *
 * <p>
 * This configuration class defines the core authentication-related components
 * used by the application. It provides the beans required for verifying user credentials,
 * encoding passwords securely, and managing the overall authentication process.
 * </p>
 * <ul>
 *   <li><b>AuthenticationManager</b>: The central interface for authenticating requests.</li>
 *   <li><b>AuthenticationProvider</b>: The component that actually processes the authentication logic.</li>
 *   <li><b>PasswordEncoder</b>: The hashing mechanism for user passwords.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthenticationConfig {

    /**
     * Defines the password encoding strategy.
     * <p>
     * The application relies on <strong>BCrypt</strong> for strongly hashing user passwords.
     * </p>
     *
     * @return a {@link BCryptPasswordEncoder} instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Initializing BCryptPasswordEncoder bean.");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
