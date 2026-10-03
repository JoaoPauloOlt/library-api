package com.jpoltramari.library_api.application.command.author;

public record UpdateAuthorCommand(
        String name,
        String nationality
) {
}
