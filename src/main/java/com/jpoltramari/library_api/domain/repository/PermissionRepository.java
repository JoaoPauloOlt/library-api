package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.Permissions;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends CustomJpaRepository<Permissions, Short>{

    Optional<Permissions> findByName(String name);
}
