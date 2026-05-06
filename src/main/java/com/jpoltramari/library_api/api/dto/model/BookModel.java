package com.jpoltramari.library_api.api.dto.model;

import lombok.Data;

@Data
public class BookModel {

    private Long id;
    private String isbn;
    private String title;
    private String genre;

    private Short totalQuantity;
    private Short availableQuantity;

    private AuthorModel author;

}
