package com.jpoltramari.library_api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpoltramari.library_api.api.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class ApiSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiSecurityExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        writeErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Authentication is required to access this resource.",
                request.getRequestURI()
        );
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException, ServletException {

        writeErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "You do not have permission to access this resource.",
                request.getRequestURI()
        );
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    HttpStatus status,
                                    String title,
                                    String detail,
                                    String path) throws IOException {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .title(title)
                .detail(detail)
                .path(path)
                .build();

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}