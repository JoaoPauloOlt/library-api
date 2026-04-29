package com.jpoltramari.library_api.domain.exception;

public class BookNotFound extends EntityNotFoundException {

    private static final long serialVersionUID = 1L;

    public BookNotFound(String message) {
        super(message);
    }

    public BookNotFound(Long isbn){
        this(String.format("there is no book with isbn %d", isbn));
    }
}
