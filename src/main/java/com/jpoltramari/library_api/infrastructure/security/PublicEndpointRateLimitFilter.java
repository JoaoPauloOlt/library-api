package com.jpoltramari.library_api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpoltramari.library_api.api.exception.ErrorResponse;
import com.jpoltramari.library_api.application.port.context.CorrelationIdPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
public class PublicEndpointRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";
    private static final String REGISTRATION_PATH = "/users";

    private final PublicRateLimitService rateLimitService;
    private final CorrelationIdPort correlationIdPort;
    private final ObjectMapper objectMapper;

    public PublicEndpointRateLimitFilter(PublicRateLimitService rateLimitService,
                                         CorrelationIdPort correlationIdPort,
                                         ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.correlationIdPort = correlationIdPort;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod())) {
            String path = request.getServletPath();
            String clientIp = request.getRemoteAddr();

            if (LOGIN_PATH.equals(path) && !rateLimitService.allowLoginFromIp(clientIp)) {
                log.warn("event=rate_limit_rejected scope=ip path={} correlationId={}",
                        path, correlationId());
                reject(request, response, rateLimitService.loginIpRetryAfterSeconds(clientIp));
                return;
            }

            if (REGISTRATION_PATH.equals(path) && !rateLimitService.allowRegistrationFromIp(clientIp)) {
                log.warn("event=rate_limit_rejected scope=ip path={} correlationId={}",
                        path, correlationId());
                reject(request, response, rateLimitService.registrationIpRetryAfterSeconds(clientIp));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response,
                        long retryAfterSeconds) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .title("Too many requests")
                .detail("Request rate limit exceeded. Please try again later.")
                .path(request.getRequestURI())
                .correlationId(correlationId())
                .errorCode("RATE_LIMIT_EXCEEDED")
                .build();

        response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        response.setHeader("Retry-After", Long.toString(Math.max(1, retryAfterSeconds)));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String correlationId() {
        return correlationIdPort.getCorrelationId().orElse(null);
    }
}
