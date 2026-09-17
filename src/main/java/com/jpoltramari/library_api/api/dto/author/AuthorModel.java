package com.jpoltramari.library_api.api.dto.author;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthorModel", description = "Author representation returned by the API.")
public record AuthorModel(
        @Schema(description = "Author identifier.", example = "1")
        Long id,
        @Schema(description = "Author name.", example = "George Orwell")
        String name,
        @Schema(description = "Author nationality.", example = "British")
        String nationality
) {}
