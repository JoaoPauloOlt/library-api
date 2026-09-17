package com.jpoltramari.library_api.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "UserModel", description = "User representation returned by the API. Sensitive credentials are never exposed.")
public record UserModel(
        @Schema(description = "User identifier.", example = "1")
        Long id,
        @Schema(description = "User full name.", example = "João Paulo")
        String name,
        @Schema(description = "User email address.", example = "joao@example.com")
        String email,
        @Schema(description = "User telephone number.", example = "11999999999")
        String telephone,
        @Schema(description = "Current user account status.", example = "ACTIVE")
        String status,
        @Schema(description = "Groups assigned to the user.", example = "[\"USER\"]")
        List<String> groups
) {}
