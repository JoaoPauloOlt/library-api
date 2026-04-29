package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.User;

import java.util.Optional;

public interface UserRepository extends CustomJpaRepository<User, Long>{

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
