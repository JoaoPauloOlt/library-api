package com.jpoltramari.library_api.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void shouldBeActiveWhenNotRevokedAndNotExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(Instant.now().plusSeconds(60));

        assertThat(token.isActive()).isTrue();
    }

    @Test
    void shouldBeInactiveWhenRevoked() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setRevokedAt(Instant.now());

        assertThat(token.isActive()).isFalse();
    }

    @Test
    void shouldBeInactiveWhenExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(Instant.now().minusSeconds(1));

        assertThat(token.isActive()).isFalse();
    }
}
