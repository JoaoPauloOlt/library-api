package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.Permission;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims;
import com.jpoltramari.library_api.infrastructure.security.rbac.RbacResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityCoreCoverageTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBuildAuthenticatedUserFromClaimsAndExposeUserDetails() {
        JwtClaims claims = claims(42L, List.of("USER"), List.of("BOOK_READ"));
        AuthenticatedUser user = AuthenticatedUser.fromClaims(claims);

        assertThat(user.getUserId()).isEqualTo(42L);
        assertThat(user.getEmail()).isEqualTo("user@library.com");
        assertThat(user.getName()).isEqualTo("Library User");
        assertThat(user.getGroups()).containsExactly("USER");
        assertThat(user.getPermissions()).containsExactly("BOOK_READ");
        assertThat(user.getTokenVersion()).isZero();
        assertThat(user.hasPermission("BOOK_READ")).isTrue();
        assertThat(user.hasPermission("BOOK_DELETE")).isFalse();
        assertThat(user.getUsername()).isEqualTo("user@library.com");
        assertThat(user.getPassword()).isNull();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.getAuthorities()).hasSize(1);
    }

    @Test
    void shouldBuildAuthenticatedUserFromActiveAndInactiveUsers() {
        User active = user(UserStatus.ACTIVE);
        active.getGroups().add(groupWithPermission("LIBRARIAN", "BOOK_READ"));

        AuthenticatedUser activePrincipal = AuthenticatedUser.fromUser(active, new RbacResolver());
        assertThat(activePrincipal.isEnabled()).isTrue();
        assertThat(activePrincipal.getAuthorities()).hasSize(1);

        User inactive = user(UserStatus.INACTIVE);
        AuthenticatedUser inactivePrincipal = AuthenticatedUser.fromUser(inactive, new RbacResolver());
        assertThat(inactivePrincipal.isEnabled()).isFalse();
    }

    @Test
    void shouldCreateJwtAuthenticationTokenAndExposeClaims() {
        AuthenticatedUser principal = AuthenticatedUser.fromClaims(claims(42L, List.of("USER"), List.of("BOOK_READ")));
        JwtClaims claims = claims(42L, List.of("USER"), List.of("BOOK_READ"));

        JwtAuthenticationToken token = new JwtAuthenticationToken(principal, claims);

        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getPrincipal()).isSameAs(principal);
        assertThat(token.getCredentials()).isSameAs(claims);
        assertThat(token.getClaims()).isSameAs(claims);
        assertThat(token.getAuthorities()).hasSize(1);
    }

    @Test
    void shouldResolveCurrentUserFromSecurityContext() {
        AuthenticatedUser principal = AuthenticatedUser.fromClaims(claims(99L, List.of("USER"), List.of()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertThat(SecurityUtils.getCurrentUser()).containsSame(principal);
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(99L);
    }

    @Test
    void shouldRejectMissingOrUnexpectedSecurityPrincipal() {
        assertThat(SecurityUtils.getCurrentUser()).isEmpty();
        assertThatThrownBy(SecurityUtils::getCurrentUserId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user in context");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null)
        );
        assertThat(SecurityUtils.getCurrentUser()).isEmpty();
    }

    private JwtClaims claims(Long userId, List<String> groups, List<String> permissions) {
        return new JwtClaims(
                "jti-1",
                userId,
                "user@library.com",
                "Library User",
                groups,
                permissions,
                0,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "library-api",
                "library-api-clients"
        );
    }

    private User user(UserStatus status) {
        User user = new User();
        user.setId(10L);
        user.setName("Library User");
        user.setEmail("user@library.com");
        user.setPassword("encoded-password");
        user.setTelephone("11999999999");
        user.setStatus(status);
        user.setTokenVersion(0);
        return user;
    }

    private Group groupWithPermission(String name, String permissionName) {
        Group group = new Group();
        group.setName(name);
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName(permissionName);
        group.getPermissions().add(permission);
        return group;
    }
}
