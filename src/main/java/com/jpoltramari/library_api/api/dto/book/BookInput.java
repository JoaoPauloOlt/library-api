package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.domain.enums.Genre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "BookInput", description = "Data required to create a book.")
public record BookInput(
        @Schema(description = "International Standard Book Number with exactly 13 digits.", example = "9780451524935", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Pattern(regexp = "\\d{13}", message = "must contain exactly 13 digits")
        String isbn,

        @Schema(description = "Book title.", example = "1984", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank
        @Size(max = 150)
        String title,

        @Schema(description = "Book genre.", example = "FICTION", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Genre genre,

        @Schema(description = "Optional book synopsis or description.", example = "A dystopian novel about surveillance and authoritarian control.", maxLength = 5000)
        @Size(max = 5000)
        String description,

        @Schema(description = "Optional HTTP(S) URL for the book cover.", example = "https://books.google.com/books/content?id=example&printsec=frontcover", maxLength = 500)
        @Size(max = 500)
        @Pattern(regexp = "https?://.+", message = "must be a valid HTTP(S) URL")
        String coverUrl,

        @Schema(description = "Initial number of physical copies to create.", example = "5", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(value = 0, message = "must be greater than or equal to 0")
        Integer quantity,

        @Schema(description = "Identifiers of the authors associated with the book.", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
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
