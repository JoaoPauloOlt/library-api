package com.jpoltramari.library_api.application.command.user;

public record CreateUserCommand(
        String name,
        String email,
        String telephone,
        String password
) {
}
