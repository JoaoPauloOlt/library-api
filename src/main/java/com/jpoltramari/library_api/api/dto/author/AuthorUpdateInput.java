package com.jpoltramari.library_api.api.dto.author;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "AuthorUpdateInput", description = "Fields that can be changed when updating an author. All fields are optional.")
public record AuthorUpdateInput(
        @Schema(description = "Author name.", example = "George Orwell", maxLength = 100)
        @Size(max = 100)
        String name,

        @Schema(description = "Author nationality.", example = "British", maxLength = 40)
        @Size(max = 40)
        String nationality
) {}
