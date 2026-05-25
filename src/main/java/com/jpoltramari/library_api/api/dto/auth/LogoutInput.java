package com.jpoltramari.library_api.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutInput(

        @NotBlank
        String refreshToken,

        @NotBlank
        String accessToken
) {}
