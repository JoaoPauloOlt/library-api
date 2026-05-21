package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaimsBuilder;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtProperties;
import com.jpoltramari.library_api.infrastructure.security.jwt.TokenBlacklist;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {

    private final JwtProperties properties;
    private final JwtClaimsBuilder claimsBuilder;
    private final TokenBlacklist tokenBlacklist;

    public JwtService(
            JwtProperties properties,
            JwtClaimsBuilder claimsBuilder,
            TokenBlacklist tokenBlacklist
    ) {
        this.properties = properties;
        this.claimsBuilder = claimsBuilder;
        this.tokenBlacklist = tokenBlacklist;
    }

    public String generateToken(User user) {
        return claimsBuilder.buildAccessToken(user);
    }

    public long getExpiration() {
        return properties.getExpiration();
    }

    /**
     * Parses and validates the token exactly once.
     */
    public Optional<JwtClaims> parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(claimsBuilder.signingKey())
                    .requireIssuer(properties.getIssuer())
                    .requireAudience(properties.getAudience())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String jti = claims.getId();
            if (jti != null && tokenBlacklist.isBlacklisted(jti)) {
                log.warn("event=jwt_rejected reason=blacklisted jti={} correlationId={}",
                        jti, correlationId());
                return Optional.empty();
            }

            return Optional.of(toJwtClaims(claims));

        } catch (ExpiredJwtException ex) {
            log.debug("event=jwt_expired subject={} correlationId={}",
                    ex.getClaims().getSubject(), correlationId());
        } catch (SignatureException ex) {
            log.warn("event=jwt_invalid reason=bad_signature correlationId={}", correlationId());
        } catch (MalformedJwtException | UnsupportedJwtException ex) {
            log.warn("event=jwt_invalid reason=malformed message={} correlationId={}",
                    ex.getMessage(), correlationId());
        } catch (JwtException ex) {
            log.warn("event=jwt_invalid reason=validation_failed message={} correlationId={}",
                    ex.getMessage(), correlationId());
        }
        return Optional.empty();
    }

    public void blacklistAccessToken(String token) {
        parseAndValidate(token).ifPresent(claims -> {
            if (claims.jti() != null && claims.expiresAt() != null) {
                tokenBlacklist.blacklist(claims.jti(), claims.expiresAt());
                log.info("event=access_token_blacklisted jti={} userId={} correlationId={}",
                        claims.jti(), claims.userId(), correlationId());
            }
        });
    }

    private JwtClaims toJwtClaims(Claims claims) {
        Long userId = resolveUserId(claims);
        String email = claims.get("email", String.class);
        if (email == null) {
            email = claims.getSubject();
        }

        return new JwtClaims(
                claims.getId(),
                userId,
                email,
                claims.get("name", String.class),
                stringList(claims.get("groups")),
                stringList(claims.get("permissions")),
                claims.get("tokenVersion", Integer.class),
                claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null,
                claims.getExpiration() != null ? claims.getExpiration().toInstant() : null,
                claims.getIssuer(),
                claims.getAudience()
        );
    }

    private Long resolveUserId(Claims claims) {
        Long userId = claims.get("userId", Long.class);
        if (userId != null) {
            return userId;
        }
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return Collections.emptyList();
    }

    private static String correlationId() {
        return MDC.get(com.jpoltramari.library_api.infrastructure.security.filter.CorrelationIdFilter.MDC_KEY);
    }
}
