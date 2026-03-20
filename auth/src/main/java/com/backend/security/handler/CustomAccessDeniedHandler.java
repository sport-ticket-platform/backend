package com.backend.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * <h2>Error 403</h2>
 * <p>Intercepts authenticated users who are trying to access resources above their pay grade.</p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String jsonResponse = "{\"error\": \"Forbidden\", \"message\": \"You do not have the required permissions to perform this action.\"}";
        response.getWriter().write(jsonResponse);
    }
}
