package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.api.dto.author.AuthorModel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "BookModel", description = "Book representation returned by the API.")
public record BookModel(
        @Schema(description = "Book identifier.", example = "1")
        Long id,
        @Schema(description = "13-digit ISBN.", example = "9780451524935")
        String isbn,
        @Schema(description = "Book title.", example = "1984")
        String title,
        @Schema(description = "Book genre.", example = "CLASSIC")
        String genre,
        @Schema(description = "Book creation timestamp.", example = "2026-09-16T14:30:00")
        LocalDateTime createdAt,
        @Schema(description = "HTTP(S) URL of the book cover.", example = "https://books.google.com/books/content?id=example&printsec=frontcover")
        String coverUrl,
        @Schema(description = "Book synopsis or description.", example = "A dystopian novel about surveillance and authoritarian control.")
        String description,
        @Schema(description = "Total number of physical copies.", example = "5")
        Long totalCopies,
        @Schema(description = "Number of physical copies currently available.", example = "3")
        Long availableCopies,
        @Schema(description = "Total number of recorded loans for this book.", example = "12")
        Long loanCount,
        @Schema(description = "Authors associated with the book.")
        List<AuthorModel> authors
) {}
