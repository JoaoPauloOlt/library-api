package com.jpoltramari.library_api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void shouldAllowBearerAuthorizationHeaderWithoutCredentialedCors() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://library.example");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/loans"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isFalse();
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type", "X-Correlation-Id");
        assertThat(cors.getAllowedMethods()).contains("PATCH");
        assertThat(cors.getExposedHeaders()).contains("Authorization", "X-Correlation-Id");
        assertThat(cors.getAllowedOriginPatterns()).containsExactly("https://library.example");
    }
}
