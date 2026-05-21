package com.jpoltramari.library_api.api.dto.auth;

public record LogoutInput(
        String refreshToken,
        String accessToken
) {}
