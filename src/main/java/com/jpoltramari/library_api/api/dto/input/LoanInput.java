package com.jpoltramari.library_api.api.dto.input;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanInput {

    @NotNull
    private Long bookId;

    @NotNull
    private Long userId;

}