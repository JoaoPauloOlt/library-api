package com.jpoltramari.library_api.api.dto.author;

import jakarta.validation.constraints.Size;

public record AuthorUpdateInput(

        @Size(max = 100)
        String name,

        @Size(max = 40)
        String nationality
) {}
