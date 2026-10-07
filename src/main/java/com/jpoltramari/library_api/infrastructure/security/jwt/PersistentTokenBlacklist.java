package com.jpoltramari.library_api.infrastructure.security.jwt;

import com.jpoltramari.library_api.domain.model.RevokedAccessToken;
import com.jpoltramari.library_api.domain.repository.RevokedAccessTokenRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PersistentTokenBlacklist implements TokenBlacklist {

    private final RevokedAccessTokenRepository repository;

    public PersistentTokenBlacklist(RevokedAccessTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }

        repository.deleteByExpiresAtLessThanEqual(Instant.now());
        return repository.existsByJtiAndExpiresAtAfter(jti, Instant.now());
    }

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) {
            return;
        }

        repository.save(new RevokedAccessToken(jti, expiresAt));
    }

    @Override
    public void blacklist(String jti) {
        blacklist(jti, Instant.now().plusSeconds(3600));
    }
}
