package com.jpoltramari.library_api.application.result.auth;

import com.jpoltramari.library_api.domain.enums.UserStatus;

public record UserSecuritySnapshot(
        Long userId,
        Integer tokenVersion,
        UserStatus status
) {
}
