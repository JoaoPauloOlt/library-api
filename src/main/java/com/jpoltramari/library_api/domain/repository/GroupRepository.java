package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.Group;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends CustomJpaRepository<Group, Short> {

    Optional<Group> findByName(String name);
}
