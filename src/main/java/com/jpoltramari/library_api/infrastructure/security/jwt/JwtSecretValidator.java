package com.jpoltramari.library_api.infrastructure.security.jwt;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtSecretValidator {

    private static final int MIN_KEY_BYTES = 32;

    private final JwtProperties properties;

    public JwtSecretValidator(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret must be configured");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must contain at least " + MIN_KEY_BYTES + " UTF-8 bytes (256 bits) for HS256"
            );
        }
    }
}
