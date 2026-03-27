package com.backend.security.handler;

import com.backend.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime; // و البته این

/**
 * <h2>Error 403</h2>
 * <p>Intercepts authenticated users who are trying to access resources above their pay grade.</p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiResponse<?> errorResponse = ApiResponse.builder()
                .status(HttpServletResponse.SC_FORBIDDEN)
                .success(false)
                .title("Forbidden")
                .message("You do not have the required permissions to access this resource")
                .timestamp(LocalDateTime.now())
                .titleFa("دسترسی ممنوع")
                .messageFa("شما مجوزهای لازم برای دسترسی به این منبع را ندارید")
                .build();

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
