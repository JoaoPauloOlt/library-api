package com.jpoltramari.library_api.infrastructure.security.snapshot;

import java.util.Optional;

public interface UserSecuritySnapshotPort {

    Optional<UserSecuritySnapshot> findByUserId(Long userId);

    void evict(Long userId);
}
