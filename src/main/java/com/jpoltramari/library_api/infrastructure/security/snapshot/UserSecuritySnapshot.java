package com.jpoltramari.library_api.infrastructure.security.snapshot;

import com.jpoltramari.library_api.domain.enums.UserStatus;

public record UserSecuritySnapshot(
        Long userId,
        Integer tokenVersion,
        UserStatus status
) {

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
