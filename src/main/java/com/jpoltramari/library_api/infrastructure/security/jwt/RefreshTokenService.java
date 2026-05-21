package com.jpoltramari.library_api.infrastructure.security.jwt;

import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.model.RefreshToken;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.RefreshTokenRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.domain.service.TokenVersionService;
import com.jpoltramari.library_api.infrastructure.security.AuthenticatedUser;
import com.jpoltramari.library_api.infrastructure.security.rbac.RbacResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtProperties properties;
    private final JwtClaimsBuilder claimsBuilder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenVersionService tokenVersionService;
    private final RbacResolver rbacResolver;

    @Transactional
    public IssuedTokens issueTokens(User user) {
        String accessToken = claimsBuilder.buildAccessToken(user);
        String refreshToken = createRefreshToken(user);
        return new IssuedTokens(accessToken, refreshToken, buildPrincipal(user));
    }

    @Transactional
    public Optional<IssuedTokens> rotate(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);

        Optional<RefreshToken> stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash);
        if (stored.isEmpty()) {
            log.warn("event=refresh_rejected reason=token_not_found");
            return Optional.empty();
        }

        RefreshToken current = stored.get();

        if (!current.isActive()) {
            log.warn("event=refresh_rejected reason=token_expired userId={}", current.getUserId());
            revokeFamilyAndBumpVersion(current.getUserId());
            return Optional.empty();
        }

        if (current.getReplacedByJti() != null) {
            log.warn("event=refresh_rejected reason=token_reuse_detected userId={}", current.getUserId());
            revokeFamilyAndBumpVersion(current.getUserId());
            return Optional.empty();
        }

        current.setRevokedAt(Instant.now());
        String newJti = UUID.randomUUID().toString();
        current.setReplacedByJti(newJti);
        refreshTokenRepository.save(current);

        User user = userRepository.findByIdWithGroupsAndPermissions(current.getUserId())
                .orElse(null);

        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            log.warn("event=refresh_rejected reason=user_inactive_or_missing userId={}",
                    current.getUserId());
            return Optional.empty();
        }

        String newRefreshRaw = persistRefreshToken(user.getId(), newJti);
        String accessToken = claimsBuilder.buildAccessToken(user);
        AuthenticatedUser principal = AuthenticatedUser.fromUser(user, rbacResolver);

        return Optional.of(new IssuedTokens(accessToken, newRefreshRaw, principal));
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    public long getRefreshExpiration() {
        return properties.getRefreshExpiration();
    }

    private String createRefreshToken(User user) {
        String jti = UUID.randomUUID().toString();
        return persistRefreshToken(user.getId(), jti);
    }

    private String persistRefreshToken(Long userId, String jti) {
        String raw = jti + "." + UUID.randomUUID();
        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setJti(jti);
        entity.setTokenHash(hashToken(raw));
        entity.setExpiresAt(Instant.now().plusMillis(properties.getRefreshExpiration()));
        refreshTokenRepository.save(entity);
        return raw;
    }

    private void revokeFamilyAndBumpVersion(Long userId) {
        tokenVersionService.revokeAllSessions(userId);
    }

    private AuthenticatedUser buildPrincipal(User user) {
        return AuthenticatedUser.fromUser(user, rbacResolver);
    }

    public static String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record IssuedTokens(
            String accessToken,
            String refreshToken,
            AuthenticatedUser principal
    ) {}
}
