package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.user.RegistrationResponse;
import com.jpoltramari.library_api.api.dto.user.UserInput;
import com.jpoltramari.library_api.api.mapper.UserMapper;
import com.jpoltramari.library_api.application.command.user.CreateUserCommand;
import com.jpoltramari.library_api.application.service.UserService;
import com.jpoltramari.library_api.domain.exception.EmailAlreadyRegisteredException;
import com.jpoltramari.library_api.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerRegistrationTest {

    @Mock private UserService service;
    @Mock private UserMapper mapper;
    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(service, mapper);
    }

    @Test
    void shouldReturnSameGenericResponseForNewAndExistingEmail() {
        UserInput input = new UserInput("Reader", "reader@example.com", "11999999999", "StrongPassword123!");

        when(service.create(any(CreateUserCommand.class))).thenReturn(new User());
        var createdResponse = controller.create(input);

        when(service.create(any(CreateUserCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException("Email already registered."));
        var duplicateResponse = controller.create(input);

        assertEquals(200, createdResponse.getStatusCode().value());
        assertEquals(createdResponse.getStatusCode(), duplicateResponse.getStatusCode());
        assertEquals(createdResponse.getBody(), duplicateResponse.getBody());
        assertEquals(RegistrationResponse.genericConfirmation(), duplicateResponse.getBody());
    }

    @Test
    void shouldRethrowUnrelatedDatabaseIntegrityErrors() {
        UserInput input = new UserInput("Reader", "reader@example.com", "11999999999", "StrongPassword123!");
        org.springframework.dao.DataIntegrityViolationException exception =
                new org.springframework.dao.DataIntegrityViolationException("foreign key constraint");

        when(service.create(any(CreateUserCommand.class))).thenThrow(exception);

        assertSame(exception, assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> controller.create(input)));
    }
}
