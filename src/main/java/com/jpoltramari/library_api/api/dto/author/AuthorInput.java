package com.jpoltramari.library_api.api.dto.author;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AuthorInput", description = "Data required to create an author.")
public record AuthorInput(
        @Schema(description = "Author name.", example = "George Orwell", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Author nationality.", example = "British", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 40)
        @NotBlank
        @Size(max = 40)
        String nationality
) {}
