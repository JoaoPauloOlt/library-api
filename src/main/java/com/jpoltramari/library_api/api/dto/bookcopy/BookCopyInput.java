package com.jpoltramari.library_api.api.dto.bookcopy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "BookCopyInput", description = "Data required to create a physical copy of a book.")
public record BookCopyInput(
        @Schema(description = "Identifier of the book that owns the physical copy.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Long bookId,

        @Schema(description = "Physical location of the copy in the library.", example = "Shelf A-03", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank
        @Size(max = 100)
        String location
) {
}
