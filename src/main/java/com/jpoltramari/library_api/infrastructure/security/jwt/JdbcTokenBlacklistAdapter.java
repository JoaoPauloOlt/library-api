package com.jpoltramari.library_api.infrastructure.security.jwt;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class JdbcTokenBlacklistAdapter implements TokenBlacklist {

    private static final String EXISTS_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM revoked_access_tokens
                WHERE jti = ?
                  AND expires_at > CURRENT_TIMESTAMP
            )
            """;

    private static final String DELETE_EXPIRED_SQL = """
            DELETE FROM revoked_access_tokens
            WHERE expires_at <= CURRENT_TIMESTAMP
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO revoked_access_tokens (jti, expires_at)
            VALUES (?, ?)
            ON CONFLICT (jti)
            DO UPDATE SET expires_at = EXCLUDED.expires_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcTokenBlacklistAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }

        Boolean blacklisted = jdbcTemplate.queryForObject(EXISTS_SQL, Boolean.class, jti);
        return Boolean.TRUE.equals(blacklisted);
    }

    @Override
    public void blacklist(String jti) {
        blacklist(jti, Instant.now().plusSeconds(3600));
    }

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            return;
        }

        jdbcTemplate.update(DELETE_EXPIRED_SQL);
        jdbcTemplate.update(UPSERT_SQL, jti, Timestamp.from(expiresAt));
    }
}
