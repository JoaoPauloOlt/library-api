package com.jpoltramari.library_api.api.dto.loan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "LoanForUserInput", description = "Data required for a staff member to create a loan for an active user.")
public record LoanForUserInput(
        @Schema(description = "Identifier of the book to borrow.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Long bookId,

        @Schema(description = "Identifier of the active borrower.", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Long userId
) {}
