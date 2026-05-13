package com.jpoltramari.library_api.api.dto.input;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookInput {

    @NotBlank
    @Size(min = 13, max = 13)
    private String isbn;

    @NotBlank
    private String title;

    @NotBlank
    private String genre;

    @NotNull
    @Positive
    private Integer totalQuantity;

    @NotEmpty
    private List<Long> authorIds;
}
