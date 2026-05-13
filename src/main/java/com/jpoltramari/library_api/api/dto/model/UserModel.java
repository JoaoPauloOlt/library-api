package com.jpoltramari.library_api.api.dto.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserModel {

    private Long id;
    private String name;
    private String email;
    private String telephone;
    private String status;

    private List<String> groups;
}
