package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.Book;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends CustomJpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    List<Book> findAllByTitleContaining(String title);
    Optional<Book> findAllByTitle(String title);
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
}
