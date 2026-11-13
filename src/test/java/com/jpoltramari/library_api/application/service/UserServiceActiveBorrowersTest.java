package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.GroupRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceActiveBorrowersTest {

    @Mock private UserRepository repository;
    @Mock private GroupRepository groupRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, groupRepository, passwordEncoder);
    }

    @Test
    void shouldReturnOnlyTheActiveBorrowerPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> expected = new PageImpl<>(List.of(new User()));
        when(repository.findByStatus(UserStatus.ACTIVE, pageable)).thenReturn(expected);

        Page<User> result = service.findActiveBorrowers(pageable);

        assertSame(expected, result);
        verify(repository).findByStatus(UserStatus.ACTIVE, pageable);
    }
}
