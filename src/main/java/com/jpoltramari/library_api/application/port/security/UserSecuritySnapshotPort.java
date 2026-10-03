package com.jpoltramari.library_api.application.port.security;

import com.jpoltramari.library_api.application.result.auth.UserSecuritySnapshot;

import java.util.Optional;

public interface UserSecuritySnapshotPort {

    Optional<UserSecuritySnapshot> findByUserId(Long userId);

    void evict(Long userId);
}
