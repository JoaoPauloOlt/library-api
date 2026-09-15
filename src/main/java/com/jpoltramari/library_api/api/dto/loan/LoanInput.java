package com.jpoltramari.library_api.api.dto.loan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LoanInput(

        @NotNull
        @Positive
        Long bookId
) {}
