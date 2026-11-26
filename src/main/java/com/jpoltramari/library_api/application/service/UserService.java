package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.user.CreateUserCommand;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.EmailAlreadyRegisteredException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.exception.UserNotFoundException;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.GroupRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.domain.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String DEFAULT_GROUP = "USER";
    private final UserRepository repository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<User> findAll(Pageable pageable) { return repository.findAll(pageable); }

    @PreAuthorize("hasAuthority('LOAN_CREATE_FOR_USER')")
    public Page<User> findActiveBorrowers(Pageable pageable) {
        return repository.findByStatus(UserStatus.ACTIVE, pageable);
    }

    public User findOrFail(Long id) {
        return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User create(CreateUserCommand command) {
        String normalizedEmail = EmailNormalizer.normalize(command.email());
        // Keep the expensive password hash and common database lookup on both paths
        // before checking whether the email is already registered.
        String encodedPassword = passwordEncoder.encode(command.password());
        Group defaultGroup = findDefaultGroup();
        validateEmail(normalizedEmail);
        User user = new User();
        user.setName(command.name());
        user.setEmail(normalizedEmail);
        user.setTelephone(command.telephone());
        user.setPassword(encodedPassword);
        user.setStatus(UserStatus.ACTIVE);
        user.setGroups(Set.of(defaultGroup));
        return repository.save(user);
    }

    private void validateEmail(String email) {
        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Email already registered.");
        }
    }

    private Group findDefaultGroup() {
        return groupRepository.findByName(DEFAULT_GROUP).orElseThrow(() ->
                new EntityNotFoundException("Default group USER was not found."));
    }
}
