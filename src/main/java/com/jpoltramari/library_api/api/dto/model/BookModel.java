package com.jpoltramari.library_api.api.dto.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookModel {

    private Long id;
    private String isbn;
    private String title;
    private String genre;

    private Short totalQuantity;
    private Short availableQuantity;

    private List<AuthorModel> authors;

}
