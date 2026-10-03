package com.jpoltramari.library_api.application.result.auth;

public record AuthenticationResult(
        String accessToken,
        String refreshToken,
        Long expiresIn
) {
}
