package com.jpoltramari.library_api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpoltramari.library_api.api.exception.ErrorResponse;
import com.jpoltramari.library_api.infrastructure.security.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
public class ApiSecurityExceptionHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public ApiSecurityExceptionHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ex
    ) throws IOException {

        log.warn("event=authentication_required path={} correlationId={}",
                request.getRequestURI(), correlationId());

        write(response, HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Authentication required. Provide a valid Bearer token.",
                "AUTHENTICATION_REQUIRED",
                request.getRequestURI());
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex
    ) throws IOException {

        log.warn("event=access_denied path={} message={} correlationId={}",
                request.getRequestURI(), ex.getMessage(), correlationId());

        write(response, HttpStatus.FORBIDDEN,
                "Forbidden",
                ex.getMessage() != null ? ex.getMessage() : "Insufficient permissions for this resource",
                "ACCESS_DENIED",
                request.getRequestURI());
    }

    private void write(HttpServletResponse response,
                       HttpStatus status,
                       String title,
                       String detail,
                       String errorCode,
                       String path) throws IOException {

        var body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .title(title)
                .detail(detail)
                .path(path)
                .correlationId(correlationId())
                .errorCode(errorCode)
                .build();

        response.setStatus(status.value());
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), body);
    }

    private static String correlationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
