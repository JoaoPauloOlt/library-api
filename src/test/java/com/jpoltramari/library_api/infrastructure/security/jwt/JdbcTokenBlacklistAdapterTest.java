package com.jpoltramari.library_api.infrastructure.security.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcTokenBlacklistAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JdbcTokenBlacklistAdapter adapter;

    @Test
    void shouldReturnTrueWhenTokenIsPersistentlyBlacklisted() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq("revoked-jti")))
                .thenReturn(true);

        assertThat(adapter.isBlacklisted("revoked-jti")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTokenIsNotBlacklisted() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq("valid-jti")))
                .thenReturn(false);

        assertThat(adapter.isBlacklisted("valid-jti")).isFalse();
    }

    @Test
    void shouldReturnFalseForNullJtiWithoutQueryingDatabase() {
        assertThat(adapter.isBlacklisted(null)).isFalse();

        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Boolean.class), any());
    }

    @Test
    void shouldPersistRevocationAndPurgeExpiredEntries() {
        Instant expiresAt = Instant.now().plusSeconds(600);

        adapter.blacklist("revoked-jti", expiresAt);

        verify(jdbcTemplate).update(
                "DELETE FROM revoked_access_tokens WHERE expires_at <= CURRENT_TIMESTAMP"
        );
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("INSERT INTO revoked_access_tokens"),
                eq("revoked-jti"),
                any(java.time.OffsetDateTime.class)
        );
    }

    @Test
    void shouldIgnoreNullOrExpiredRevocations() {
        adapter.blacklist(null, Instant.now().plusSeconds(600));
        adapter.blacklist("expired-jti", Instant.now().minusSeconds(1));
        adapter.blacklist("missing-expiration", null);

        verify(jdbcTemplate, never()).update(anyString(), any(Object.class), any(Object.class));
    }

    @Test
    void shouldUseDefaultExpirationWhenOnlyJtiIsProvided() {
        adapter.blacklist("default-expiration-jti");

        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("INSERT INTO revoked_access_tokens"),
                eq("default-expiration-jti"),
                any(java.sql.Timestamp.class)
        );
    }
}
