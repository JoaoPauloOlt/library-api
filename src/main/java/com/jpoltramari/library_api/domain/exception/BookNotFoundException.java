package com.jpoltramari.library_api.domain.exception;

public class BookNotFoundException extends EntityNotFoundException {

    public BookNotFoundException(Long id) {
        super(String.format("Book with id %d was not found", id));
    }
}
