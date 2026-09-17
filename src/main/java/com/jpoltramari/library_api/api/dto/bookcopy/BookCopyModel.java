package com.jpoltramari.library_api.api.dto.bookcopy;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BookCopyModel", description = "Physical book copy representation returned by the API.")
public record BookCopyModel(
        @Schema(description = "Physical copy identifier.", example = "1")
        Long id,
        @Schema(description = "Unique barcode generated for the physical copy.", example = "BK-550e8400-e29b-41d4-a716-446655440000")
        String barcode,
        @Schema(description = "Current physical copy status.", example = "AVAILABLE")
        String status,
        @Schema(description = "Physical location of the copy.", example = "Shelf A-03")
        String location,
        @Schema(description = "Whether the copy is active.", example = "true")
        boolean active,
        @Schema(description = "Identifier of the associated book.", example = "1")
        Long bookId,
        @Schema(description = "Title of the associated book.", example = "1984")
        String bookTitle
) {
}
