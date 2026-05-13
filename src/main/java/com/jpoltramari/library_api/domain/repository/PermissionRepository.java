package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.Permission;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends CustomJpaRepository<Permission, Long>{

    Optional<Permission> findByName(String name);
}
