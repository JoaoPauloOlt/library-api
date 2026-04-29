package com.jpoltramari.library_api.domain.exception;

public class AuthorNotFound extends EntityNotFoundException {

    private static final long serialVersionUID =1L;

    public AuthorNotFound(String message) {
        super(message);
    }

    public AuthorNotFound(Long authorId){
        this(String.format("there is no author with code %d", authorId));
    }
}