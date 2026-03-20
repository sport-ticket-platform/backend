package com.backend.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * <h2>Error 401</h2>
 * <p>Handles unauthenticated requests instead of throwing Spring default HTML error pages.</p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Component
public class CustomAuthEntryPointHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException
    ) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String jsonResponse = String.format(
                "{\"error\": \"Unauthorized\", \"message\": \"Authentication is required to access this resource. Details: %s\"}",
                "Please provide a valid token."
        );

        response.getWriter().write(jsonResponse);
    }
}
