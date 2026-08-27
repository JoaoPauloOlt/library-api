package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.api.dto.author.AuthorModel;
import java.time.LocalDateTime;
import java.util.List;

public record BookModel(
        Long id,
        String isbn,
        String title,
        String genre,
        LocalDateTime createdAt,
        String coverUrl,
        String description,
        Long totalCopies,
        Long availableCopies,
        Long loanCount,
        List<AuthorModel> authors
) {}
