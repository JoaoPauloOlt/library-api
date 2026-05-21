package com.jpoltramari.library_api.api.dto.author;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorInput(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 40)
        String nationality
) {}
