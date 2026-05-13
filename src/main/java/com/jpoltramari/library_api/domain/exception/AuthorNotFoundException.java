package com.jpoltramari.library_api.domain.exception;

public class AuthorNotFoundException extends EntityNotFoundException {

    public AuthorNotFoundException(Long id){
        super(String.format("Author with id %d was not found", id));
    }
}