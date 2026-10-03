package com.jpoltramari.library_api.application.port.security;

import com.jpoltramari.library_api.domain.model.User;

import java.util.Optional;

public interface RefreshTokenPort {

    IssuedTokens issueTokens(User user);

    Optional<IssuedTokens> rotate(String rawRefreshToken);

    void revoke(String rawRefreshToken);

    record IssuedTokens(
            String accessToken,
            String refreshToken
    ) {
    }
}
