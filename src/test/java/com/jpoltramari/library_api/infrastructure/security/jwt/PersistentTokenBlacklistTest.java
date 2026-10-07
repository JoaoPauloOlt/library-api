package com.jpoltramari.library_api.infrastructure.security.jwt;

import com.jpoltramari.library_api.domain.model.RevokedAccessToken;
import com.jpoltramari.library_api.domain.repository.RevokedAccessTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PersistentTokenBlacklistTest {

    private RevokedAccessTokenRepository repository;
    private PersistentTokenBlacklist blacklist;

    @BeforeEach
    void setUp() {
        repository = mock(RevokedAccessTokenRepository.class);
        blacklist = new PersistentTokenBlacklist(repository);
    }

    @Test
    void shouldReturnFalseForNullJti() {
        assertThat(blacklist.isBlacklisted(null)).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnTrueWhenJtiExistsAndHasNotExpired() {
        when(repository.existsByJtiAndExpiresAtAfter(eq("jti-1"), any(Instant.class)))
                .thenReturn(true);

        assertThat(blacklist.isBlacklisted("jti-1")).isTrue();

        verify(repository).deleteByExpiresAtLessThanEqual(any(Instant.class));
    }

    @Test
    void shouldReturnFalseWhenJtiDoesNotExist() {
        when(repository.existsByJtiAndExpiresAtAfter(eq("jti-2"), any(Instant.class)))
                .thenReturn(false);

        assertThat(blacklist.isBlacklisted("jti-2")).isFalse();
    }

    @Test
    void shouldPersistJtiWithExpiration() {
        Instant expiresAt = Instant.now().plusSeconds(300);

        blacklist.blacklist("jti-3", expiresAt);

        verify(repository).save(new RevokedAccessToken("jti-3", expiresAt));
    }

    @Test
    void shouldIgnoreInvalidBlacklistArguments() {
        blacklist.blacklist(null, Instant.now());
        blacklist.blacklist("jti-4", null);

        verifyNoInteractions(repository);
    }
}
