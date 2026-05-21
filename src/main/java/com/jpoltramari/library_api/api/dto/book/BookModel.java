package com.jpoltramari.library_api.api.dto.book;

import com.jpoltramari.library_api.api.dto.author.AuthorModel;

import java.util.List;

public record BookModel(

        Long id,
        String isbn,
        String title,
        String genre,

        Integer totalQuantity,
        Integer availableQuantity,

        List<AuthorModel> authors
) {}
