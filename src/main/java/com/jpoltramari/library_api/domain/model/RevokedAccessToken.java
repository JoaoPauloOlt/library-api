package com.jpoltramari.library_api.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "revoked_access_tokens",
        indexes = @Index(name = "idx_revoked_access_tokens_expires_at", columnList = "expires_at")
)
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RevokedAccessToken {

    @Id
    @Column(length = 36, nullable = false)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
