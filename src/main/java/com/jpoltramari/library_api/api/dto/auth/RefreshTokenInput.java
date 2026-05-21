package com.jpoltramari.library_api.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenInput(
        @NotBlank String refreshToken
) {}
