package com.jpoltramari.library_api.infrastructure.security.jwt;

import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.infrastructure.security.rbac.RbacResolver;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtClaimsBuilder {

    private final JwtProperties properties;
    private final RbacResolver rbacResolver;
    private final Key signingKey;

    public JwtClaimsBuilder(JwtProperties properties, RbacResolver rbacResolver) {
        this.properties = properties;
        this.rbacResolver = rbacResolver;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String buildAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(properties.getExpiration());
        String jti = UUID.randomUUID().toString();

        List<String> groups = rbacResolver.resolveGroups(user);
        List<String> permissions = rbacResolver.resolvePermissions(user);
        int tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;

        return Jwts.builder()
                .setId(jti)
                .setSubject(String.valueOf(user.getId()))
                .setIssuer(properties.getIssuer())
                .setAudience(properties.getAudience())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("groups", groups)
                .claim("permissions", permissions)
                .claim("tokenVersion", tokenVersion)
                .signWith(signingKey)
                .compact();
    }

    public Key signingKey() {
        return signingKey;
    }
}
