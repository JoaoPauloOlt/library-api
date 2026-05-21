package com.jpoltramari.library_api.infrastructure.security.jwt;

import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

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

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("jwt.secret must be a valid Base64-encoded key", ex);
        }

        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least " + MIN_KEY_BYTES + " bytes (256 bits) for HS256"
            );
        }
    }
}
