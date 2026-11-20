package com.jpoltramari.library_api.infrastructure.security.snapshot;

import com.jpoltramari.library_api.application.port.security.UserSecuritySnapshotPort;
import com.jpoltramari.library_api.application.result.auth.UserSecuritySnapshot;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads the current authorization snapshot directly from the primary database.
 * Avoids instance-local caching so security changes are visible across API instances.
 */
@Component
public class DatabaseUserSecuritySnapshotAdapter implements UserSecuritySnapshotPort {

    private final UserRepository userRepository;

    public DatabaseUserSecuritySnapshotAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSecuritySnapshot> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        return userRepository.findSecuritySnapshotById(userId)
                .map(view -> new UserSecuritySnapshot(
                        view.getId(),
                        view.getTokenVersion(),
                        view.getStatus()
                ));
    }
}
