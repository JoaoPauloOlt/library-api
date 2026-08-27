package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.domain.enums.Genre;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BookUpdateInput(
        @Size(min = 13, max = 13) String isbn,
        @Size(max = 150) String title,
        Genre genre,
        @Size(max = 5000) String description,
        @Size(max = 500) String coverUrl,
        List<Long> authorIds
) {}
