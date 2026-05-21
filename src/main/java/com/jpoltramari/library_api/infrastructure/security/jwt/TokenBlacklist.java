package com.jpoltramari.library_api.infrastructure.security.jwt;

/**
 * Extension point for token revocation (Redis, in-memory, etc.).
 */
public interface TokenBlacklist {

    boolean isBlacklisted(String jti);

    void blacklist(String jti);

    void blacklist(String jti, java.time.Instant expiresAt);
}
