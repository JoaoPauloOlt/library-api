package com.jpoltramari.library_api.api.exception;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class ValidationErrorResponse {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String path;
    private List<Field> fields;

}