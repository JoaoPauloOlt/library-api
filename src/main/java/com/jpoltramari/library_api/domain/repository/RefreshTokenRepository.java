package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends CustomJpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.revokedAt = :revokedAt
        where rt.userId = :userId and rt.revokedAt is null
    """)
    void revokeAllByUserId(Long userId, Instant revokedAt);

    @Modifying
    @Query("""
        delete from RefreshToken rt
        where rt.expiresAt < :before or rt.revokedAt < :before
    """)
    int deleteExpiredOrRevokedBefore(Instant before);
}
