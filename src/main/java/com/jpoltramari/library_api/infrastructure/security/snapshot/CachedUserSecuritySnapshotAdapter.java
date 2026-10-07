package com.jpoltramari.library_api.infrastructure.security.snapshot;

import com.jpoltramari.library_api.application.port.security.UserSecuritySnapshotPort;
import com.jpoltramari.library_api.application.result.auth.UserSecuritySnapshot;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CachedUserSecuritySnapshotAdapter implements UserSecuritySnapshotPort {

    private final UserRepository userRepository;

    public CachedUserSecuritySnapshotAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSecuritySnapshot> findByUserId(Long userId) {
        return userRepository.findSecuritySnapshotById(userId)
                .map(view -> new UserSecuritySnapshot(
                        view.getId(),
                        view.getTokenVersion(),
                        view.getStatus()
                ));
    }

    @Override
    public void evict(Long userId) {
        // Kept for the port contract; snapshots are read directly from the shared database.
    }
}
