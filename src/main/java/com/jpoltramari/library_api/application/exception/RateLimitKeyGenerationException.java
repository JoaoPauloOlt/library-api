package com.jpoltramari.library_api.application.exception;

public class RateLimitKeyGenerationException extends RuntimeException {

    public RateLimitKeyGenerationException(Throwable cause) {
        super("Unable to create account rate-limit key", cause);
    }
}
