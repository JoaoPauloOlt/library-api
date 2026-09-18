package com.jpoltramari.library_api.infrastructure.security.jwt;

import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.RefreshTokenRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.domain.repository.UserSecuritySnapshotView;
import com.jpoltramari.library_api.infrastructure.security.JwtService;
import com.jpoltramari.library_api.domain.service.TokenVersionService;
import com.jpoltramari.library_api.infrastructure.security.rbac.RbacResolver;
import com.jpoltramari.library_api.infrastructure.security.snapshot.CachedUserSecuritySnapshotAdapter;
import com.jpoltramari.library_api.infrastructure.security.snapshot.UserSecuritySnapshot;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;\nimport io.jsonwebtoken.io.DecodingException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JwtInfrastructureCoverageTest {

    private static final String SECRET = "bGlicmFyeS1hcGktY2ktc2VjcmV0LXRlc3QtMjAyNi0wOC0yNA==";

    @Test
    void shouldBuildAndParseJwtAndRejectBlacklistedToken() {
        JwtProperties properties = properties();
        JwtClaimsBuilder builder = new JwtClaimsBuilder(properties, new RbacResolver());
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        JwtService service = new JwtService(properties, builder, blacklist);

        User user = user(UserStatus.ACTIVE);
        String token = service.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(service.getExpiration()).isEqualTo(properties.getExpiration());
        assertThat(service.parseAndValidate(token))
                .get()
                .satisfies(claims -> {
                    assertThat(claims.userId()).isEqualTo(10L);
                    assertThat(claims.email()).isEqualTo("user@library.com");
                    assertThat(claims.name()).isEqualTo("Library User");
                    assertThat(claims.groups()).isEmpty();
                    assertThat(claims.permissions()).isEmpty();
                    assertThat(claims.tokenVersion()).isZero();
                    assertThat(claims.issuer()).isEqualTo("library-api");
                    assertThat(claims.audience()).isEqualTo("library-api-clients");
                });

        service.blacklistAccessToken(token);
        assertThat(service.parseAndValidate(token)).isEmpty();
    }

    @Test
    void shouldRejectMalformedAndBadSignatureTokens() {
        JwtProperties properties = properties();
        JwtClaimsBuilder builder = new JwtClaimsBuilder(properties, new RbacResolver());
        JwtService service = new JwtService(properties, builder, new InMemoryTokenBlacklist());

        assertThat(service.parseAndValidate("not-a-jwt")).isEmpty();

        JwtProperties otherProperties = properties();
        otherProperties.setSecret("c2Vjb25kLXNlY3JldC1rZXktd2l0aC1hdC1sZWFzdC0zMi1ieXRlcw==");
        JwtClaimsBuilder otherBuilder = new JwtClaimsBuilder(otherProperties, new RbacResolver());
        String token = otherBuilder.buildAccessToken(user(UserStatus.ACTIVE));

        assertThat(service.parseAndValidate(token)).isEmpty();
    }

    @Test
    void shouldHandleBlacklistEntriesForAllStates() {
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();

        assertThat(blacklist.isBlacklisted(null)).isFalse();
        assertThat(blacklist.isBlacklisted("missing")).isFalse();

        blacklist.blacklist(null, Instant.now().plusSeconds(60));
        blacklist.blacklist("ignored", null);
        assertThat(blacklist.isBlacklisted("ignored")).isFalse();

        blacklist.blacklist("active", Instant.now().plusSeconds(60));
        assertThat(blacklist.isBlacklisted("active")).isTrue();

        blacklist.blacklist("expired", Instant.now().minusSeconds(1));
        assertThat(blacklist.isBlacklisted("expired")).isFalse();
        assertThat(blacklist.isBlacklisted("expired")).isFalse();

        blacklist.blacklist("default-expiration");
        assertThat(blacklist.isBlacklisted("default-expiration")).isTrue();
    }

    @Test
    void shouldValidateJwtSecretConfiguration() {
        JwtProperties valid = properties();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(new JwtSecretValidator(valid), "validate");

        JwtProperties blank = properties();
        blank.setSecret(" ");
        assertThatThrownBy(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(new JwtSecretValidator(blank), "validate"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("jwt.secret must be configured");

        JwtProperties invalidBase64 = properties();
        invalidBase64.setSecret("%%%");
        assertThatThrownBy(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(new JwtSecretValidator(invalidBase64), "validate"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("jwt.secret must be a valid Base64-encoded key");

        JwtProperties shortKey = properties();
        shortKey.setSecret("c2hvcnQ=");
        assertThatThrownBy(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(new JwtSecretValidator(shortKey), "validate"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void shouldCacheAndEvictSecuritySnapshots() {
        UserRepository repository = mock(UserRepository.class);
        CachedUserSecuritySnapshotAdapter adapter = new CachedUserSecuritySnapshotAdapter(repository);
        UserSecuritySnapshotView view = mock(UserSecuritySnapshotView.class);

        when(view.getId()).thenReturn(10L);
        when(view.getTokenVersion()).thenReturn(3);
        when(view.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(repository.findSecuritySnapshotById(10L)).thenReturn(Optional.of(view));

        assertThat(adapter.findByUserId(10L))
                .contains(new UserSecuritySnapshot(10L, 3, UserStatus.ACTIVE));
        assertThat(adapter.findByUserId(10L))
                .contains(new UserSecuritySnapshot(10L, 3, UserStatus.ACTIVE));
        verify(repository, times(1)).findSecuritySnapshotById(10L);

        adapter.evict(10L);
        assertThat(adapter.findByUserId(10L)).isPresent();
        verify(repository, times(2)).findSecuritySnapshotById(10L);

        when(repository.findSecuritySnapshotById(99L)).thenReturn(Optional.empty());
        assertThat(adapter.findByUserId(99L)).isEmpty();
    }

    @Test
    void shouldValidateTokenVersionAgainstSecuritySnapshot() {
        var port = mock(com.jpoltramari.library_api.infrastructure.security.snapshot.UserSecuritySnapshotPort.class);
        var validator = new com.jpoltramari.library_api.infrastructure.security.TokenVersionValidator(port);

        var validClaims = new com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims(
                "jti", 10L, "user@library.com", "User", java.util.List.of(), java.util.List.of(),
                2, Instant.now(), Instant.now().plusSeconds(3600), "library-api", "library-api-clients"
        );

        when(port.findByUserId(10L)).thenReturn(Optional.of(
                new UserSecuritySnapshot(10L, 2, UserStatus.ACTIVE)
        ));
        assertThat(validator.validate(validClaims))
                .isEqualTo(com.jpoltramari.library_api.infrastructure.security.TokenVersionValidator.ValidationResult.VALID);

        when(port.findByUserId(10L)).thenReturn(Optional.empty());
        assertThat(validator.validate(validClaims))
                .isEqualTo(com.jpoltramari.library_api.infrastructure.security.TokenVersionValidator.ValidationResult.INVALID);

        when(port.findByUserId(10L)).thenReturn(Optional.of(
                new UserSecuritySnapshot(10L, 2, UserStatus.INACTIVE)
        ));
        assertThat(validator.validate(validClaims))
                .isEqualTo(com.jpoltramari.library_api.infrastructure.security.TokenVersionValidator.ValidationResult.INACTIVE);

        when(port.findByUserId(10L)).thenReturn(Optional.of(
                new UserSecuritySnapshot(10L, 3, UserStatus.ACTIVE)
        ));
        assertThat(validator.validate(validClaims))
                .isEqualTo(com.jpoltramari.library_api.infrastructure.security.TokenVersionValidator.ValidationResult.VERSION_MISMATCH);

        var missingUserId = new com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims(
                "jti", null, "user@library.com", "User", java.util.List.of(), java.util.List.of(),
                2, Instant.now(), Instant.now().plusSeconds(3600), "library-api", "library-api-clients"
        );
        assertThat(validator.validate(missingUserId))
                .isEqualTo(com.jpoltramari.library_api.infrastructure.security.TokenVersionValidator.ValidationResult.INVALID);
    }

    private JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("library-api");
        properties.setAudience("library-api-clients");
        properties.setExpiration(3_600_000L);
        properties.setRefreshExpiration(86_400_000L);
        return properties;
    }

    private User user(UserStatus status) {
        User user = new User();
        user.setId(10L);
        user.setName("Library User");
        user.setEmail("user@library.com");
        user.setPassword("encoded");
        user.setTelephone("11999999999");
        user.setStatus(status);
        user.setTokenVersion(0);
        return user;
    }
}
