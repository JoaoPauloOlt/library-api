package com.jpoltramari.library_api.infrastructure.security.jwt;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PersistentTokenBlacklist implements TokenBlacklist {

    private final JdbcTemplate jdbcTemplate;

    public PersistentTokenBlacklist(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }

        jdbcTemplate.update(
                "delete from revoked_access_tokens where expires_at <= current_timestamp"
        );

        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from revoked_access_tokens
                where jti = ? and expires_at > current_timestamp
                """,
                Integer.class,
                jti
        );

        return count != null && count > 0;
    }

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) {
            return;
        }

        jdbcTemplate.update(
                """
                insert into revoked_access_tokens (jti, expires_at)
                values (?, ?)
                on conflict (jti)
                do update set expires_at = excluded.expires_at
                """,
                jti,
                expiresAt
        );
    }

    @Override
    public void blacklist(String jti) {
        blacklist(jti, Instant.now().plusSeconds(3600));
    }
}
