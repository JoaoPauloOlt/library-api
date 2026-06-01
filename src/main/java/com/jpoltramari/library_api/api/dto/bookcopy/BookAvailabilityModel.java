package com.jpoltramari.library_api.api.dto.bookcopy;

public record BookAvailabilityModel(
        Long bookId,
        Long totalCopies,
        Long availableCopies
) {
}
