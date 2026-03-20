package com.backend.security.config;

import com.backend.config.ApplicationProperties;
import com.backend.security.handler.CustomAccessDeniedHandler;
import com.backend.security.handler.CustomAuthEntryPointHandler;
import com.backend.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * <h2>Core Security Configuration</h2>
 * <p>
 * Configures the main <b>SecurityFilterChain</b>. This class applies our custom
 * JWT filter, exception handlers (401 & 403), and dictates the session management policy.
 * </p>
 * <ul>
 *   <li><i>CSRF:</i> Disabled for stateless APIs.</li>
 *   <li><i>Session:</i> Completely Stateless.</li>
 *   <li><i>Public Paths:</i> Dynamically loaded from application properties.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthEntryPointHandler customAuthEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final ApplicationProperties applicationProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Initializing SecurityFilterChain with Stateless Session Policy...");

        // Convert the List of strings into an array for Spring Security varargs
        String[] publicEndpoints = applicationProperties.getPublicPaths();
        log.info("Public endpoints: {}", Arrays.toString(publicEndpoints));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpoints).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
