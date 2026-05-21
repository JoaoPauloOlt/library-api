package com.jpoltramari.library_api.infrastructure.security.snapshot;

import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CachedUserSecuritySnapshotAdapter implements UserSecuritySnapshotPort {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    public CachedUserSecuritySnapshotAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSecuritySnapshot> findByUserId(Long userId) {
        CacheEntry entry = cache.get(userId);
        if (entry != null && !entry.isExpired()) {
            return Optional.of(entry.snapshot());
        }

        return userRepository.findSecuritySnapshotById(userId)
                .map(view -> new UserSecuritySnapshot(
                        view.getId(),
                        view.getTokenVersion(),
                        view.getStatus()
                ))
                .map(snapshot -> {
                    cache.put(userId, new CacheEntry(snapshot, Instant.now().plus(TTL)));
                    return snapshot;
                });
    }

    @Override
    public void evict(Long userId) {
        cache.remove(userId);
    }

    private record CacheEntry(UserSecuritySnapshot snapshot, Instant expiresAt) {

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
