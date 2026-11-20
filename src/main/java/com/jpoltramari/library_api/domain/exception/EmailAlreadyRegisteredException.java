package com.jpoltramari.library_api.domain.exception;

public class EmailAlreadyRegisteredException extends BusinessException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
