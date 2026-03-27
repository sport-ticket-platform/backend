package com.backend.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * <h2>Error 401</h2>
 * <p>Handles unauthenticated requests professionally with a structured JSON response.</p>
 */
@Component
@RequiredArgsConstructor
public class CustomAuthEntryPointHandler implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException
    ) throws IOException {

        String jwtError = (String) request.getAttribute("jwt_error_message");
        String detailMessage = (jwtError != null) ? jwtError : "Please provide a valid token.";

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .success(false)
                .title("Unauthorized")
                .message("Authentication is required to access this resource. Details: " + detailMessage)
                .timestamp(LocalDateTime.now())
                .titleFa("عدم دسترسی")
                .messageFa("برای دسترسی به این منبع باید احراز هویت شوید. جزئیات: " + detailMessage)
                .build();

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
