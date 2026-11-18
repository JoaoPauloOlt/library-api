package com.jpoltramari.library_api.domain.exception;
public class FineNotFoundException extends EntityNotFoundException {
 public FineNotFoundException(Long id) { super(String.format("Fine with id %d was not found", id)); }
}