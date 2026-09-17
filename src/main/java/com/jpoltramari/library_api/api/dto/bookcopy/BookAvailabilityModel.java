package com.jpoltramari.library_api.api.dto.bookcopy;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BookAvailabilityModel", description = "Physical copy availability summary for a book.")
public record BookAvailabilityModel(
        @Schema(description = "Book identifier.", example = "1")
        Long bookId,
        @Schema(description = "Total number of physical copies.", example = "5")
        Long totalCopies,
        @Schema(description = "Number of currently available physical copies.", example = "3")
        Long availableCopies
) {
}
