package com.jpoltramari.library_api.api.dto.auth;

public record LoginResponse(
        String token,
        String refreshToken,
        Long expiresIn
) {}
