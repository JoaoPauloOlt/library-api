package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims;
import com.jpoltramari.library_api.infrastructure.security.rbac.RbacResolver;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal stored in {@link org.springframework.security.core.context.SecurityContext}.
 * Built from JWT claims on each request (no DB) or from {@link User} at login.
 */
public final class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final String email;
    private final String name;
    private final List<String> groups;
    private final List<String> permissions;
    private final Integer tokenVersion;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final String password;

    private AuthenticatedUser(
            Long userId,
            String email,
            String name,
            List<String> groups,
            List<String> permissions,
            Integer tokenVersion,
            Collection<? extends GrantedAuthority> authorities,
            boolean enabled,
            String password
    ) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.groups = List.copyOf(groups);
        this.permissions = List.copyOf(permissions);
        this.tokenVersion = tokenVersion;
        this.authorities = authorities;
        this.enabled = enabled;
        this.password = password;
    }

    public static AuthenticatedUser fromClaims(JwtClaims claims) {
        var authorities = claims.authorities();

        return new AuthenticatedUser(
                claims.userId(),
                claims.email(),
                claims.name(),
                claims.groups(),
                claims.permissions(),
                claims.tokenVersion(),
                authorities,
                true,
                null
        );
    }

    public static AuthenticatedUser fromUser(User user, RbacResolver rbacResolver) {
        List<String> groups = rbacResolver.resolveGroups(user);
        List<String> permissions = rbacResolver.resolvePermissions(user);

        var authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();

        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getName(),
                groups,
                permissions,
                user.getTokenVersion(),
                authorities,
                user.getStatus() == UserStatus.ACTIVE,
                user.getPassword()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
