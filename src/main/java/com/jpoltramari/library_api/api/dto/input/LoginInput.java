package com.jpoltramari.library_api.api.dto.input;

import lombok.Data;

@Data
public class LoginInput {
    private String email;
    private String password;
}