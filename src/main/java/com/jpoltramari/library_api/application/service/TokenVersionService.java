package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.port.security.SessionRevocationPort;
import com.jpoltramari.library_api.application.port.security.UserSecuritySnapshotPort;
import com.jpoltramari.library_api.domain.repository.RefreshTokenRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenVersionService implements SessionRevocationPort {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSecuritySnapshotPort snapshotPort;

    @Override
    @Transactional
    public void revokeAllSessions(Long userId) {
        userRepository.incrementTokenVersion(userId);
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        snapshotPort.evict(userId);
    }
}
