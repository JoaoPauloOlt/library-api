package com.jpoltramari.library_api.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AuthenticatedUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser()
                .map(AuthenticatedUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }
}
