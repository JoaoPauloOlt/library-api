package com.jpoltramari.library_api.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginInput(

        @Email
        @NotBlank
        String email,

        @NotBlank
        String password
) {}
