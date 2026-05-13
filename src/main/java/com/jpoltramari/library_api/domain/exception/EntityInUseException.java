package com.jpoltramari.library_api.domain.exception;

public class EntityInUseException extends BusinessException {

    public EntityInUseException(String message) {
        super(message);
    }
}
