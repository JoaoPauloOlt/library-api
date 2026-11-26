package com.jpoltramari.library_api.infrastructure.security;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpoltramari.library_api.application.port.context.CorrelationIdPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PublicEndpointRateLimitFilterConfigurationTest {

    @Test
    void shouldRegisterRateLimitFilterBeforeSecurityFilters() {
        PublicRateLimitService service = new PublicRateLimitService(
                30, Duration.ofMinutes(1),
                5, Duration.ofHours(1),
                5, Duration.ofMinutes(15),
                "test-hmac-secret");

        FilterRegistrationBean<PublicEndpointRateLimitFilter> registration =
                new PublicEndpointRateLimitFilterConfiguration()
                        .publicEndpointRateLimitFilterRegistration(
                                service, () -> java.util.Optional.of("test-correlation-id"),
                                new ObjectMapper());

        assertNotNull(registration.getFilter());
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 10, registration.getOrder());
    }
}
