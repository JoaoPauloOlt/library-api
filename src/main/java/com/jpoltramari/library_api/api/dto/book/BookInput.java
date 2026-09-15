package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.domain.enums.Genre;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookInput(
        @NotBlank
        @Pattern(regexp = "\\d{13}", message = "must contain exactly 13 digits")
        String isbn,

        @NotBlank
        @Size(max = 150)
        String title,

        @NotNull
        Genre genre,

        @Size(max = 5000)
        String description,

        @Size(max = 500)
        @Pattern(regexp = "https?://.+", message = "must be a valid HTTP(S) URL")
        String coverUrl,

        @NotNull
        @Min(value = 0, message = "must be greater than or equal to 0")
        Integer quantity,

        @NotEmpty
        List<@NotNull @Positive Long> authorIds
) {
    public BookInput(String isbn, String title, Genre genre, String coverUrl, List<Long> authorIds) {
        this(isbn, title, genre, null, coverUrl, 0, authorIds);
    }

    public BookInput(String isbn, String title, Genre genre, String coverUrl, Integer quantity, List<Long> authorIds) {
        this(isbn, title, genre, null, coverUrl, quantity, authorIds);
    }
}
