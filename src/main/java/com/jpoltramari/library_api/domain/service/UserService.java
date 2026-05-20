package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.UserInput;
import com.jpoltramari.library_api.api.mapper.UserMapper;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.exception.UserNotFoundException;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.GroupRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UserMapper mapper;

    public Page<User> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public User findOrFail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User create(UserInput input) {
        validateEmail(input.getEmail());

        User user = mapper.toEntity(input);
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setGroups(Set.of(findDefaultGroup()));

        return repository.save(user);
    }

    private void validateEmail(String email) {
        if (repository.existsByEmail(email)) {
            throw new BusinessException("Email already registered.");
        }
    }

    private Group findDefaultGroup() {
        return groupRepository.findByName(DEFAULT_GROUP)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Default group USER was not found."
                        )
                );
    }
}