package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.UserInput;
import com.jpoltramari.library_api.api.mapper.UserMapper;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.exception.UserNotFoundException;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.enums.Role;
import com.jpoltramari.library_api.domain.repository.GroupRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder encoder;
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

        if (repository.existsByEmail(input.getEmail())){
            throw new BusinessException("Email already registered");
        }

        User user = mapper.toEntity(input);
        user.setPassword(encoder.encode(user.getPassword()));
        user.setStatus(UserStatus.ACTIVE);

        Group defaultGroup = groupRepository.findByName("USER")
                .orElseThrow(() -> new EntityNotFoundException("Default group USER was not found"));

        user.setGroups(Set.of(defaultGroup));

        return repository.save(user);
    }
}