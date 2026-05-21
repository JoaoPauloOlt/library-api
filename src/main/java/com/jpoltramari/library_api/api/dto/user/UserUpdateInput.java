package com.jpoltramari.library_api.api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateInput(

        @Size(max = 100)
        String name,

        @Email
        @Size(max = 100)
        String email,

        @Size(max = 20)
        String telephone
) {}
