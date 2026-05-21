package com.jpoltramari.library_api.infrastructure.security.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Immutable snapshot of a parsed JWT — single decode per request.
 */
public record JwtClaims(
        String jti,
        Long userId,
        String email,
        String name,
        List<String> groups,
        List<String> permissions,
        Integer tokenVersion,
        Instant issuedAt,
        Instant expiresAt,
        String issuer,
        String audience
) {

    public Collection<? extends GrantedAuthority> authorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
