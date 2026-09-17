package com.jpoltramari.library_api.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshTokenInput", description = "Refresh token used to issue a new access token.")
public record RefreshTokenInput(
        @Schema(description = "Valid refresh token.", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank String refreshToken
) {}
