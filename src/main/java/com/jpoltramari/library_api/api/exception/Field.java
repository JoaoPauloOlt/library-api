package com.jpoltramari.library_api.api.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Field {

    private String name;
    private String message;

    public Field(String name, String message) {
        this.name = name;
        this.message = message;
    }

}