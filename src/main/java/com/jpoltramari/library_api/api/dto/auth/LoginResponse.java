package com.jpoltramari.library_api.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "JWT authentication response.")
public record LoginResponse(
        @Schema(description = "JWT access token used to authenticate API requests.", example = "eyJhbGciOiJIUzI1NiJ9...", format = "password")
        String token,
        @Schema(description = "Refresh token used to obtain a new access token.", example = "eyJhbGciOiJIUzI1NiJ9...", format = "password")
        String refreshToken,
        @Schema(description = "Access token lifetime in seconds.", example = "3600")
        Long expiresIn
) {}
