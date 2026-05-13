package com.jpoltramari.library_api.domain.exception;

public class UserNotFoundException extends EntityNotFoundException {

    public UserNotFoundException(Long id) {
        super(String.format("User with id %d was not found", id));
    }
}
