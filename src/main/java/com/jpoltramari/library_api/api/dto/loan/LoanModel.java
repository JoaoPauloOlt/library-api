package com.jpoltramari.library_api.api.dto.loan;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "LoanModel", description = "Loan representation returned by the API.")
public record LoanModel(
        @Schema(description = "Loan identifier.", example = "1")
        Long id,
        @Schema(description = "Current loan lifecycle status.", example = "ACTIVE")
        String status,

        @Schema(description = "Title of the borrowed book.", example = "1984")
        String bookTitle,
        @Schema(description = "Cover URL of the borrowed book.", example = "https://books.google.com/books/content?id=example&printsec=frontcover")
        String bookCoverUrl,
        @Schema(description = "Names of the book authors.", example = "[\"George Orwell\"]")
        List<String> bookAuthors,
        @Schema(description = "Name of the user who owns the loan.", example = "João Paulo")
        String userName,

        @Schema(description = "Date and time when the loan was requested.", example = "2026-09-16T14:30:00")
        LocalDateTime requestDate,
        @Schema(description = "Date and time when the loan was approved.", example = "2026-09-16T15:00:00")
        LocalDateTime approvalDate,
        @Schema(description = "Date and time when the book was withdrawn.", example = "2026-09-16T16:00:00")
        LocalDateTime withdrawDate,
        @Schema(description = "Due date and time for returning the book.", example = "2026-09-23T16:00:00")
        LocalDateTime dueDate,
        @Schema(description = "Date and time when the book was returned.", example = "2026-09-22T17:00:00")
        LocalDateTime returnDate
) {}
