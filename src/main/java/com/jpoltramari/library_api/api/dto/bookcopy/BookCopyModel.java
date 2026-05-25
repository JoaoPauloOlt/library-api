package com.jpoltramari.library_api.api.dto.bookcopy;

public record BookCopyModel(

        Long id,
        String barcode,
        String status,
        String location,
        boolean active,

        Long bookId,
        String bookTitle
) {}
