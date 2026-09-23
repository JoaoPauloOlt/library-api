package com.jpoltramari.library_api.infrastructure.security.jwt;

import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.model.RefreshToken;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.RefreshTokenRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.domain.service.TokenVersionService;
import com.jpoltramari.library_api.infrastructure.security.rbac.RbacResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String RAW_TOKEN = "refresh-token";
    private static final String HASH = RefreshTokenService.hashToken(RAW_TOKEN);

    @Mock private JwtProperties properties;
    @Mock private JwtClaimsBuilder claimsBuilder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private TokenVersionService tokenVersionService;
    @Mock private RbacResolver rbacResolver;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(
                properties, claimsBuilder, refreshTokenRepository,
                userRepository, tokenVersionService, rbacResolver
        );
    }

    @Test
    void shouldIssueAccessAndRefreshTokens() {
        User user = activeUser();
        when(properties.getRefreshExpiration()).thenReturn(86_400_000L);
        when(claimsBuilder.buildAccessToken(user)).thenReturn("access-token");

        RefreshTokenService.IssuedTokens result = service.issueTokens(user);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.principal()).isNotNull();
        verify(claimsBuilder).buildAccessToken(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldReturnEmptyWhenRefreshTokenDoesNotExist() {
        when(refreshTokenRepository.findByTokenHash(HASH))
                .thenReturn(Optional.empty());

        assertThat(service.rotate(RAW_TOKEN)).isEmpty();
        verifyNoInteractions(userRepository, claimsBuilder, tokenVersionService);
    }

    @Test
    void shouldRevokeFamilyWhenRefreshTokenIsExpired() {
        RefreshToken token = storedToken();
        token.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(HASH))
                .thenReturn(Optional.of(token));

        assertThat(service.rotate(RAW_TOKEN)).isEmpty();

        verify(tokenVersionService).revokeAllSessions(42L);
        verifyNoInteractions(userRepository, claimsBuilder);
    }

    @Test
    void shouldRevokeFamilyWhenRefreshTokenWasAlreadyReplaced() {
        RefreshToken token = storedToken();
        token.setReplacedByJti("previous-jti");
        when(refreshTokenRepository.findByTokenHash(HASH))
                .thenReturn(Optional.of(token));

        assertThat(service.rotate(RAW_TOKEN)).isEmpty();

        verify(tokenVersionService).revokeAllSessions(42L);
    }

    @Test
    void shouldRejectRefreshWhenUserIsMissing() {
        when(refreshTokenRepository.findByTokenHash(HASH))
                .thenReturn(Optional.of(storedToken()));
        when(userRepository.findByIdWithGroupsAndPermissions(42L))
                .thenReturn(Optional.empty());

        assertThat(service.rotate(RAW_TOKEN)).isEmpty();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectRefreshWhenUserIsInactive() {
        User user = activeUser();
        user.setStatus(UserStatus.INACTIVE);
        when(refreshTokenRepository.findByTokenHash(HASH))
                .thenReturn(Optional.of(storedToken()));
        when(userRepository.findByIdWithGroupsAndPermissions(42L))
                .thenReturn(Optional.of(user));

        assertThat(service.rotate(RAW_TOKEN)).isEmpty();
    }

    @Test
    void shouldRotateValidRefreshToken() {
        User user = activeUser();
        RefreshToken token = storedToken();
        when(properties.getRefreshExpiration()).thenReturn(86_400_000L);

        when(refreshTokenRepository.findByTokenHash(HASH))
                .thenReturn(Optional.of(token));
        when(userRepository.findByIdWithGroupsAndPermissions(42L))
                .thenReturn(Optional.of(user));
        when(claimsBuilder.buildAccessToken(user)).thenReturn("new-access-token");

        Optional<RefreshTokenService.IssuedTokens> result = service.rotate(RAW_TOKEN);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().accessToken()).isEqualTo("new-access-token");
        assertThat(result.orElseThrow().refreshToken()).isNotBlank();
        assertThat(token.getRevokedAt()).isNotNull();
        assertThat(token.getReplacedByJti()).isNotBlank();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void shouldRevokeExistingRefreshToken() {
        RefreshToken token = storedToken();
        when(refreshTokenRepository.findByTokenHash(HASH))
                .thenReturn(Optional.of(token));

        service.revoke(RAW_TOKEN);

        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void shouldDoNothingWhenRevokingUnknownRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(HASH))
                .thenReturn(Optional.empty());

        service.revoke(RAW_TOKEN);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldExposeConfiguredRefreshExpiration() {
        when(properties.getRefreshExpiration()).thenReturn(86_400_000L);
        assertThat(service.getRefreshExpiration()).isEqualTo(86_400_000L);
    }

    @Test
    void shouldHashTokenDeterministically() {
        assertThat(RefreshTokenService.hashToken("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private User activeUser() {
        User user = new User();
        user.setId(42L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(0);
        return user;
    }

    private RefreshToken storedToken() {
        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setUserId(42L);
        token.setJti("old-jti");
        token.setTokenHash(HASH);
        token.setExpiresAt(Instant.now().plusSeconds(300));
        return token;
    }
}
