package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.user.CreateUserCommand;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.GroupRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository repository;
    @Mock private GroupRepository groupRepository;
    @Mock private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() { service = new UserService(repository, groupRepository, passwordEncoder); }

    @Test
    void shouldFindUserOrFail() {
        User user = new User();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        assertEquals(user, service.findOrFail(1L));
    }

    @Test
    void shouldCreateActiveUserWithDefaultGroupAndEncodedPassword() {
        CreateUserCommand command = new CreateUserCommand(
                "John", "john@example.com", "11999999999", "password123");
        Group group = new Group();
        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("encoded");
        when(groupRepository.findByName("USER")).thenReturn(Optional.of(group));
        when(repository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.create(command);

        assertEquals("John", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("11999999999", result.getTelephone());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        assertEquals("encoded", result.getPassword());
        assertEquals(1, result.getGroups().size());
        verify(repository).save(result);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        CreateUserCommand command = new CreateUserCommand(
                "John", "john@example.com", "11999999999", "password123");
        when(repository.existsByEmail(command.email())).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(command));
    }

    @Test
    void shouldRejectWhenDefaultGroupDoesNotExist() {
        CreateUserCommand command = new CreateUserCommand(
                "John", "john@example.com", "11999999999", "password123");
        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(groupRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.create(command));
    }
}
