package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.User;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends CustomJpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        select distinct u
        from User u
        left join fetch u.groups g
        left join fetch g.permissions
        where u.email = :email
    """)
    Optional<User> findByEmailWithGroupsAndPermissions(String email);
}
