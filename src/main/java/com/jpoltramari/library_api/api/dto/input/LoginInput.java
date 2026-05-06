package com.jpoltramari.library_api.api.dto.input;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginInput {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}