package com.jpoltramari.library_api.api.dto.input;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorInput {

    @NotBlank
    private String name;

    @NotBlank
    private String nationality;
}