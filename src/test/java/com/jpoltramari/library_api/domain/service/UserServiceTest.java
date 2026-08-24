package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.user.UserInput;
import com.jpoltramari.library_api.api.mapper.UserMapper;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository repository;
    @Mock private GroupRepository groupRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper mapper;
    private UserService service;

    @BeforeEach
    void setUp() { service = new UserService(repository, groupRepository, passwordEncoder, mapper); }

    @Test
    void shouldFindUserOrFail() {
        User user = new User();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        assertEquals(user, service.findOrFail(1L));
    }

    @Test
    void shouldCreateActiveUserWithDefaultGroupAndEncodedPassword() {
        UserInput input = new UserInput("John", "john@example.com", "11999999999", "password123");
        User user = new User();
        Group group = new Group();
        when(repository.existsByEmail(input.email())).thenReturn(false);
        when(mapper.toEntity(input)).thenReturn(user);
        when(passwordEncoder.encode(input.password())).thenReturn("encoded");
        when(groupRepository.findByName("USER")).thenReturn(Optional.of(group));
        when(repository.save(user)).thenReturn(user);

        User result = service.create(input);

        assertEquals(UserStatus.ACTIVE, result.getStatus());
        assertEquals("encoded", result.getPassword());
        assertEquals(1, result.getGroups().size());
        verify(repository).save(user);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        UserInput input = new UserInput("John", "john@example.com", "11999999999", "password123");
        when(repository.existsByEmail(input.email())).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(input));
    }

    @Test
    void shouldRejectWhenDefaultGroupDoesNotExist() {
        UserInput input = new UserInput("John", "john@example.com", "11999999999", "password123");
        when(repository.existsByEmail(input.email())).thenReturn(false);
        when(mapper.toEntity(input)).thenReturn(new User());
        when(groupRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.create(input));
    }
}
