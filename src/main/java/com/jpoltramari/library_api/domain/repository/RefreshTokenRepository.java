package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

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
