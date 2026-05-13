package com.jpoltramari.library_api.domain.exception;

public class LoanNotFoundException extends EntityNotFoundException {

    public LoanNotFoundException(Long id) {
        super(String.format("Loan with id %d was not found", id));
    }
}
