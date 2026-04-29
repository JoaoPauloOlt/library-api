package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.Author;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends CustomJpaRepository<Author, Long>{

    List<Author> findAllByNameContaining(String name);
    Optional<Author> findAllByName(String name);
}
