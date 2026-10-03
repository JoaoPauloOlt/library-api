package com.jpoltramari.library_api.application.command.bookcopy;

public record CreateBookCopyCommand(
        Long bookId,
        String location
) {}
