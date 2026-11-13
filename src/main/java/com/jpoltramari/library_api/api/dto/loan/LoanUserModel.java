package com.jpoltramari.library_api.api.dto.loan;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoanUserModel", description = "Minimal active-user representation for staff circulation.")
public record LoanUserModel(
        @Schema(description = "User identifier.", example = "42")
        Long id,
        @Schema(description = "User full name.", example = "João Paulo")
        String name,
        @Schema(description = "User email address for disambiguation.", example = "joao@example.com")
        String email
) {}
