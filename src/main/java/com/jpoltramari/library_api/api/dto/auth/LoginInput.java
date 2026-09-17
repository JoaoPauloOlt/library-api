package com.jpoltramari.library_api.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginInput", description = "Credentials used to authenticate a user.")
public record LoginInput(
        @Schema(description = "User email address.", example = "user@library.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @Email
        @NotBlank
        String email,

        @Schema(description = "User password.", example = "User123!", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank
        String password
) {}
