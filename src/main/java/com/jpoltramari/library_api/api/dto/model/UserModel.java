package com.jpoltramari.library_api.api.dto.model;

import lombok.Data;

import java.util.List;

@Data
public class UserModel {

    private Long id;
    private String name;
    private String email;
    private String telephone;
    private String status;

    private List<String> groups;
}
