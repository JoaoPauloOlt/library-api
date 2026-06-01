package com.jpoltramari.library_api.api.dto.bookcopy;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import jakarta.validation.constraints.Size;

public record BookCopyUpdateInput(

        CopyStatus status,

        @Size(max = 100)
        String location,

        Boolean active
) {
}
