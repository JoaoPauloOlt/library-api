package com.jpoltramari.library_api.infrastructure.security;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private final PublicRateLimitService rateLimitService;
    private final CorrelationIdPort correlationIdPort;

    public PublicEndpointRateLimitFilter(PublicRateLimitService rateLimitService,
                                         CorrelationIdPort correlationIdPort) {
        this.rateLimitService = rateLimitService;
        this.correlationIdPort = correlationIdPort;
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
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(Math.max(1, retryAfterSeconds)));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (JsonGenerator json = JSON_FACTORY.createGenerator(response.getWriter())) {
            json.writeStartObject();
            json.writeStringField("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());
            json.writeNumberField("status", HttpStatus.TOO_MANY_REQUESTS.value());
            json.writeStringField("title", "Too many requests");
            json.writeStringField("detail", "Request rate limit exceeded. Please try again later.");
            json.writeStringField("path", request.getRequestURI());
            String correlationId = correlationId();
            if (correlationId != null) {
                json.writeStringField("correlationId", correlationId);
            }
            json.writeStringField("errorCode", "RATE_LIMIT_EXCEEDED");
            json.writeEndObject();
        }
    }

    private String correlationId() {
        return correlationIdPort.getCorrelationId().orElse(null);
    }
}
