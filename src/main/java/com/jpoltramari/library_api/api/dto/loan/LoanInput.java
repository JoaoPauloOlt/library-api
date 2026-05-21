package com.jpoltramari.library_api.api.dto.loan;

import jakarta.validation.constraints.NotNull;

public record LoanInput(

        @NotNull
        Long bookId
) {}
