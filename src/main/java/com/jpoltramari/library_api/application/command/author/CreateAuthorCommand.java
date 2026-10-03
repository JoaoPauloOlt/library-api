package com.jpoltramari.library_api.application.command.author;

public record CreateAuthorCommand(
        String name,
        String nationality
) {
}
