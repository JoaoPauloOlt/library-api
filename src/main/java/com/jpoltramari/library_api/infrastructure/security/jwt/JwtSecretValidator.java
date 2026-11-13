package com.jpoltramari.library_api.infrastructure.security.jwt;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtSecretValidator {

    private static final int MIN_KEY_BYTES = 32;
    private static final String DEVELOPMENT_ONLY_SECRET =
            "library-api-local-dev-secret-2026-change-me";

    private final JwtProperties properties;
    private final Environment environment;

    public JwtSecretValidator(JwtProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret must be configured");
        }

        if (environment.acceptsProfiles(Profiles.of("prod")) && DEVELOPMENT_ONLY_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "The development JWT secret must not be used with the prod profile; configure a unique, randomly generated JWT_SECRET"
            );
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must contain at least " + MIN_KEY_BYTES + " UTF-8 bytes (256 bits) for HS256"
            );
        }
    }
}
