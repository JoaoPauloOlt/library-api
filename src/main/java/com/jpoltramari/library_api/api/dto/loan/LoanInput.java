package com.jpoltramari.library_api.api.dto.loan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "LoanInput", description = "Data required to request a loan.")
public record LoanInput(
        @Schema(description = "Identifier of the book to borrow.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Long bookId
) {}
