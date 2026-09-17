package com.jpoltramari.library_api.domain.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "BookFilter", description = "Optional filters used when listing books.")
public class BookFilter {

    @Schema(description = "Filters by partial book title.", example = "1984")
    private String title;

    @Schema(description = "Filters by genre value.", example = "CLASSIC")
    private String genre;

    @Schema(description = "Filters by partial author name.", example = "Orwell")
    private String authorName;
}
