package com.jpoltramari.library_api.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LogoutInput", description = "Tokens used to invalidate the current authentication session.")
public record LogoutInput(
        @Schema(description = "Refresh token to invalidate.", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank
        String refreshToken,

        @Schema(description = "Access token to invalidate.", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank
        String accessToken
) {}
