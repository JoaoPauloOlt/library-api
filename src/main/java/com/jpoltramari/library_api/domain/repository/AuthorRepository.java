package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<Author> findAllByNameContaining(String name);
    Optional<Author> findByName(String name);
}
