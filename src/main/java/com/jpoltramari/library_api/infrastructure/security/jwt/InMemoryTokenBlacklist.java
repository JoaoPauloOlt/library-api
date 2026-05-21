package com.jpoltramari.library_api.infrastructure.security.jwt;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final ConcurrentHashMap<String, Instant> blacklisted = new ConcurrentHashMap<>();

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Instant expiresAt = blacklisted.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            blacklisted.remove(jti);
            return false;
        }
        return true;
    }

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        if (jti != null && expiresAt != null) {
            blacklisted.put(jti, expiresAt);
        }
    }

    @Override
    public void blacklist(String jti) {
        blacklist(jti, Instant.now().plusSeconds(3600));
    }
}
