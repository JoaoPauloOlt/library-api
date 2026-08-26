package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.book.BookInput;
import com.jpoltramari.library_api.api.dto.book.BookUpdateInput;
import com.jpoltramari.library_api.api.mapper.BookMapper;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.repository.AuthorRepository;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.spec.BookSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository repository;
    private final AuthorRepository authorRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookMapper mapper;

    public Page<Book> findAll(BookFilter filter, Pageable pageable) {
        return repository.findAll(BookSpecs.usingFilter(filter), pageable);
    }

    public Book findOrFail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public Book create(BookInput input) {
        validateIsbn(input.isbn());

        Book book = mapper.toEntity(input);
        book.setAuthors(loadAuthors(input.authorIds()));

        Book savedBook = repository.save(book);
        createCopies(savedBook, input.quantity());

        return savedBook;
    }

    @Transactional
    public Book update(Long id, BookUpdateInput input) {
        Book book = findOrFail(id);

        if (input.isbn() != null && !book.getIsbn().equals(input.isbn())) {
            validateIsbn(input.isbn());
        }

        mapper.update(input, book);

        if (input.authorIds() != null) {
            book.setAuthors(loadAuthors(input.authorIds()));
        }

        return repository.save(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = findOrFail(id);

        if (!book.getCopies().isEmpty()) {
            throw new BusinessException(
                    "Cannot delete a book that has registered copies");
        }
        repository.delete(book);
    }

    private void createCopies(Book book, Integer quantity) {
        int copyQuantity = quantity == null ? 0 : quantity;

        if (copyQuantity == 0) {
            return;
        }

        List<BookCopy> copies = new ArrayList<>(copyQuantity);

        for (int i = 0; i < copyQuantity; i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setStatus(CopyStatus.AVAILABLE);
            copy.setActive(true);
            copy.setBarcode(generateBarcode());
            copies.add(copy);
        }

        bookCopyRepository.saveAll(copies);
    }

    private String generateBarcode() {
        return "BK-" + UUID.randomUUID();
    }

    private void validateIsbn(String isbn) {
        if (repository.existsByIsbn(isbn)) {
            throw new BusinessException("ISBN already registered.");
        }
    }

    private Set<Author> loadAuthors(List<Long> authorIds) {
        List<Author> authors = authorRepository.findAllById(authorIds);

        if (authors.size() != new HashSet<>(authorIds).size()) {
            throw new EntityNotFoundException(
                    "One or more authors were not found."
            );
        }

        return new HashSet<>(authors);
    }
}
