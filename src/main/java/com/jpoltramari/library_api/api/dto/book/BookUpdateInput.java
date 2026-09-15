package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.domain.enums.Genre;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookUpdateInput(
        @Pattern(regexp = "\\d{13}", message = "must contain exactly 13 digits")
        String isbn,

        @Size(max = 150)
        String title,

        Genre genre,

        @Size(max = 5000)
        String description,

        @Size(max = 500)
        @Pattern(regexp = "https?://.+", message = "must be a valid HTTP(S) URL")
        String coverUrl,

        List<@Positive Long> authorIds
) {}
