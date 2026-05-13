package com.jpoltramari.library_api.api.dto.input;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanInput {

    @NotNull
    private Long bookId;
}