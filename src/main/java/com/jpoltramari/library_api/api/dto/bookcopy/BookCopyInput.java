package com.jpoltramari.library_api.api.dto.bookcopy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookCopyInput (

        @NotNull
        Long bookId,

        @NotBlank
        @Size(max = 100)
        String location
){}
