package com.jpoltramari.library_api.infrastructure.security.jwt;

/**
 * Persistent access-token revocation contract.
 */
public interface TokenBlacklist {

    boolean isBlacklisted(String jti);

    void blacklist(String jti);

    void blacklist(String jti, java.time.Instant expiresAt);
}
