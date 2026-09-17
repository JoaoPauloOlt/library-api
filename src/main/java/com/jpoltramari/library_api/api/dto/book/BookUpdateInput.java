package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.domain.enums.Genre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "BookUpdateInput", description = "Fields that can be changed when updating a book. All fields are optional.")
public record BookUpdateInput(
        @Schema(description = "International Standard Book Number with exactly 13 digits.", example = "9780451524935")
        @Pattern(regexp = "\\d{13}", message = "must contain exactly 13 digits")
        String isbn,

        @Schema(description = "Book title.", example = "1984", maxLength = 150)
        @Size(max = 150)
        String title,

        @Schema(description = "Book genre.", example = "CLASSIC")
        Genre genre,

        @Schema(description = "Book synopsis or description.", example = "A dystopian novel about surveillance and authoritarian control.", maxLength = 5000)
        @Size(max = 5000)
        String description,

        @Schema(description = "HTTP(S) URL for the book cover.", example = "https://books.google.com/books/content?id=example&printsec=frontcover", maxLength = 500)
        @Size(max = 500)
        @Pattern(regexp = "https?://.+", message = "must be a valid HTTP(S) URL")
        String coverUrl,

        @Schema(description = "Author identifiers associated with the book.", example = "[1, 2]")
        List<@Positive Long> authorIds
) {}
