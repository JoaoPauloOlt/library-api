package com.jpoltramari.library_api.infrastructure.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PersistentTokenBlacklistTest {

    private JdbcTemplate jdbcTemplate;
    private PersistentTokenBlacklist blacklist;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        blacklist = new PersistentTokenBlacklist(jdbcTemplate);
    }

    @Test
    void shouldReturnFalseForNullJti() {
        assertThat(blacklist.isBlacklisted(null)).isFalse();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldReturnTrueWhenJtiExistsAndHasNotExpired() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("jti-1")))
                .thenReturn(1);

        assertThat(blacklist.isBlacklisted("jti-1")).isTrue();

        verify(jdbcTemplate).update(
                "delete from revoked_access_tokens where expires_at <= current_timestamp"
        );
    }

    @Test
    void shouldReturnFalseWhenJtiDoesNotExist() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("jti-2")))
                .thenReturn(0);

        assertThat(blacklist.isBlacklisted("jti-2")).isFalse();
    }

    @Test
    void shouldPersistJtiWithExpiration() {
        Instant expiresAt = Instant.now().plusSeconds(300);

        blacklist.blacklist("jti-3", expiresAt);

        verify(jdbcTemplate).update(anyString(), eq("jti-3"), eq(expiresAt));
    }

    @Test
    void shouldIgnoreInvalidBlacklistArguments() {
        blacklist.blacklist(null, Instant.now());
        blacklist.blacklist("jti-4", null);

        verifyNoInteractions(jdbcTemplate);
    }
}
