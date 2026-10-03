package com.jpoltramari.library_api.application.command.book;

import com.jpoltramari.library_api.domain.enums.Genre;

import java.util.List;

public record UpdateBookCommand(
        String isbn,
        String title,
        Genre genre,
        String description,
        String coverUrl,
        List<Long> authorIds
) {
}
