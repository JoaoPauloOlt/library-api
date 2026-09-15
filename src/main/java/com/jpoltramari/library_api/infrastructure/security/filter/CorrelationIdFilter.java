package com.jpoltramari.library_api.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);
        long startNanos = System.nanoTime();

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            logRequest(request, response, durationMs);
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(HEADER);

        if (correlationId == null || correlationId.isBlank() || !isSafeCorrelationId(correlationId)) {
            return UUID.randomUUID().toString();
        }

        return correlationId;
    }

    private boolean isSafeCorrelationId(String correlationId) {
        return correlationId.length() <= 100
                && correlationId.chars().allMatch(character ->
                Character.isLetterOrDigit(character)
                        || character == '-'
                        || character == '_'
                        || character == '.');
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        int status = response.getStatus();
        String event = status >= 500 ? "request_failed" : status >= 400 ? "request_rejected" : "request_completed";

        if (status >= 500) {
            log.error("event={} method={} path={} status={} durationMs={}",
                    event, request.getMethod(), request.getRequestURI(), status, durationMs);
        } else if (status >= 400) {
            log.warn("event={} method={} path={} status={} durationMs={}",
                    event, request.getMethod(), request.getRequestURI(), status, durationMs);
        } else {
            log.info("event={} method={} path={} status={} durationMs={}",
                    event, request.getMethod(), request.getRequestURI(), status, durationMs);
        }
    }
}
