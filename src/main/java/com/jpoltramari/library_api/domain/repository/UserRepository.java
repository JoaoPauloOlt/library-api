package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.User;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends CustomJpaRepository<User, Long>{

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.groups g
        LEFT JOIN FETCH g.permissions
        WHERE u.email = :email
""")
    Optional<User> findByEmailWithGroupsAndPermissions(String email);
}
