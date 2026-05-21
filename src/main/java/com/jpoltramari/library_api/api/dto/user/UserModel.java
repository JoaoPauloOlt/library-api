package com.jpoltramari.library_api.api.dto.user;

import java.util.List;

public record UserModel(

        Long id,
        String name,
        String email,
        String telephone,
        String status,

        List<String> groups
) {}
