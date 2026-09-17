package com.jpoltramari.library_api.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UserInput", description = "Data required to register a user account.")
public record UserInput(
        @Schema(description = "User full name.", example = "João Paulo", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "User email address. Must be unique.", example = "joao@example.com", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @Email
        @NotBlank
        @Size(max = 100)
        String email,

        @Schema(description = "User telephone number.", example = "11999999999", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
        @NotBlank
        @Size(max = 20)
        String telephone,

        @Schema(description = "Account password. Never returned by the API.", example = "StrongPassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 100, format = "password")
        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {}
