package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.domain.enums.Genre;
import jakarta.validation.constraints.*;

import java.util.List;

public record BookInput(

        @NotBlank
        @Size(min = 13, max = 13)
        String isbn,

        @NotBlank
        @Size(max = 150)
        String title,

        @NotNull
        Genre genre,

        @NotNull
        @Positive
        Integer totalQuantity,

        @NotEmpty
        List<Long> authorIds
) {}
