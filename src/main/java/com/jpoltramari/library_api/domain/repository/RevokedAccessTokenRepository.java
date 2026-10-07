package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {

    boolean existsByJtiAndExpiresAtAfter(String jti, Instant now);

    long deleteByExpiresAtLessThanEqual(Instant now);
}
