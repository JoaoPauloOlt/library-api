package com.jpoltramari.library_api.infrastructure.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidatorTest {

    private static final String DEVELOPMENT_SECRET = "library-api-local-dev-secret-2026-change-me";

    @Test
    void shouldRejectDevelopmentSecretWhenProductionProfileIsActive() {
        JwtSecretValidator validator = validator(DEVELOPMENT_SECRET, "prod");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("development JWT secret must not be used");
    }

    @Test
    void shouldAllowDevelopmentSecretWhenDevelopmentProfileIsActive() {
        JwtSecretValidator validator = validator(DEVELOPMENT_SECRET, "dev");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowCustomSecretWhenProductionProfileIsActive() {
        JwtSecretValidator validator = validator("a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef", "prod");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    private JwtSecretValidator validator(String secret, String profile) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);

        return new JwtSecretValidator(properties, environment);
    }
}
