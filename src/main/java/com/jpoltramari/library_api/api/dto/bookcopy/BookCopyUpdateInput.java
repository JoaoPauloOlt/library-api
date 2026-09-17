package com.jpoltramari.library_api.api.dto.bookcopy;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "BookCopyUpdateInput", description = "Fields that can be changed on a physical book copy. All fields are optional.")
public record BookCopyUpdateInput(
        @Schema(description = "Physical copy status.", example = "AVAILABLE")
        CopyStatus status,

        @Schema(description = "Physical location of the copy.", example = "Shelf A-03", maxLength = 100)
        @Size(max = 100)
        String location,

        @Schema(description = "Whether the physical copy is active.", example = "true")
        Boolean active
) {
}
