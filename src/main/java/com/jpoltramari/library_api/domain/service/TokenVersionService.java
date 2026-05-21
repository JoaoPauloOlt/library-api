package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.domain.repository.RefreshTokenRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.infrastructure.security.snapshot.UserSecuritySnapshotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenVersionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSecuritySnapshotPort snapshotPort;

    @Transactional
    public void revokeAllSessions(Long userId) {
        userRepository.incrementTokenVersion(userId);
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        snapshotPort.evict(userId);
    }
}
