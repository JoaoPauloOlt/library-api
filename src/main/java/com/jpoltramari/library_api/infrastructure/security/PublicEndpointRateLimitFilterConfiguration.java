package com.jpoltramari.library_api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpoltramari.library_api.application.port.context.CorrelationIdPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class PublicEndpointRateLimitFilterConfiguration {

    @Bean
    public FilterRegistrationBean<PublicEndpointRateLimitFilter> publicEndpointRateLimitFilterRegistration(
            PublicRateLimitService rateLimitService,
            CorrelationIdPort correlationIdPort,
            ObjectMapper objectMapper) {
        FilterRegistrationBean<PublicEndpointRateLimitFilter> registration =
                new FilterRegistrationBean<>(new PublicEndpointRateLimitFilter(
                        rateLimitService, correlationIdPort, objectMapper));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
