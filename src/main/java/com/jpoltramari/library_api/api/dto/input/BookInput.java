package com.jpoltramari.library_api.api.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BookInput {

    @NotBlank
    private String isbn;

    @NotBlank
    private String title;

    @NotBlank
    private String genre;

    @NotNull
    @Positive
    private Short totalQuantity;

    @NotNull
    private Long authorId;

}
