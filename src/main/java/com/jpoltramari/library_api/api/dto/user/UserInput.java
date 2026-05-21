package com.jpoltramari.library_api.api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserInput(

        @NotBlank
        @Size(max = 100)
        String name,

        @Email
        @NotBlank
        @Size(max = 100)
        String email,

        @NotBlank
        @Size(max = 20)
        String telephone,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {}
