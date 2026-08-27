package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.domain.enums.Genre;
import jakarta.validation.constraints.*;
import java.util.List;

public record BookInput(
        @NotBlank @Size(min = 13, max = 13) String isbn,
        @NotBlank @Size(max = 150) String title,
        @NotNull Genre genre,
        @Size(max = 5000) String description,
        @Size(max = 500) String coverUrl,
        @Min(0) Integer quantity,
        @NotEmpty List<Long> authorIds
) {
    public BookInput(String isbn, String title, Genre genre, String coverUrl, List<Long> authorIds) {
        this(isbn, title, genre, null, coverUrl, 0, authorIds);
    }
}
