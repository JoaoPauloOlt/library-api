package com.jpoltramari.library_api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {
    @Test
    void shouldAllowCredentialedCorsForConfiguredOriginsAndCsrfHeader() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://library.example");
        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/auth/refresh"));
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getAllowedHeaders()).contains(
                "Authorization", "Content-Type", "X-Correlation-Id", "X-XSRF-TOKEN");
        assertThat(cors.getAllowedMethods()).contains("PATCH", "POST");
        assertThat(cors.getExposedHeaders()).contains("X-Correlation-Id");
        assertThat(cors.getAllowedOriginPatterns()).containsExactly("https://library.example");
    }
}
