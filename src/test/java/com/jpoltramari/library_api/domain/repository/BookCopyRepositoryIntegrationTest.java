package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookCopyRepositoryIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldCountOnlyActiveAvailableCopiesAndReadRetiredStatuses() {
        Book book = new Book();
        book.setIsbn("9780000000999");
        book.setTitle("Book Copy Repository Integration");
        book.setGenre(Genre.CLASSIC);
        book = bookRepository.save(book);

        BookCopy activeAvailable = copy(book, CopyStatus.AVAILABLE, true);
        BookCopy inactiveAvailable = copy(book, CopyStatus.AVAILABLE, false);
        BookCopy maintenance = copy(book, CopyStatus.MAINTENANCE, true);
        BookCopy lost = copy(book, CopyStatus.LOST, false);
        BookCopy discarded = copy(book, CopyStatus.DISCARDED, false);

        bookCopyRepository.saveAll(List.of(
                activeAvailable,
                inactiveAvailable,
                maintenance,
                lost,
                discarded
        ));
        bookCopyRepository.flush();
        entityManager.clear();

        assertThat(bookCopyRepository.countByBookIdAndStatusAndActiveTrue(book.getId(), CopyStatus.AVAILABLE))
                .isEqualTo(1L);
        assertThat(bookCopyRepository.findById(lost.getId()).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.LOST);
        assertThat(bookCopyRepository.findById(discarded.getId()).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.DISCARDED);
    }

    private BookCopy copy(Book book, CopyStatus status, boolean active) {
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode("IT-BOOK-COPY-" + UUID.randomUUID());
        copy.setStatus(status);
        copy.setActive(active);
        return copy;
    }
}
