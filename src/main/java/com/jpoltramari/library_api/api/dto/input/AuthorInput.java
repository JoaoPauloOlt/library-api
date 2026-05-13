package com.jpoltramari.library_api.api.dto.input;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorInput {

    @NotBlank
    private String name;

    @NotBlank
    private String nationality;
}