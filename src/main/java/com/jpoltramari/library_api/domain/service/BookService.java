package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.BookInput;
import com.jpoltramari.library_api.api.mapper.BookMapper;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.exception.*;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.repository.AuthorRepository;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.spec.BookSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private static final String DEFAULT_LOCATION = "Shelf A";

    private final BookRepository repository;
    private final AuthorRepository authorRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookMapper mapper;

    public BookService(BookRepository repository,
                       AuthorRepository authorRepository,
                       BookCopyRepository bookCopyRepository,
                       BookMapper mapper) {
        this.repository = repository;
        this.authorRepository = authorRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.mapper = mapper;
    }

    public Page<Book> findAll(BookFilter filter, Pageable pageable) {
        return repository.findAll(BookSpecs.usingFilter(filter), pageable);
    }

    public Book findOrFail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public Book save(BookInput input) {

        if (repository.existsByIsbn(input.getIsbn())) {
            throw new BusinessException("ISBN already registered");
        }

        List<Author> authors =
                authorRepository.findAllById(input.getAuthorIds());

        if (authors.size() != input.getAuthorIds().size()) {
            throw new EntityNotFoundException(
                    "One or more authors were not found"
            );
        }

        Book book = mapper.toEntity(input);
        book.setAuthors(authors);

        repository.save(book);

        for (int i = 1; i <= input.getTotalQuantity(); i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setBarcode(generateBarcode(book.getId(), i));
            copy.setStatus(CopyStatus.AVAILABLE);
            copy.setLocation(DEFAULT_LOCATION);

            bookCopyRepository.save(copy);
        }

        return book;
    }

    @Transactional
    public Book update(Long id, BookInput input) {
        Book book = findOrFail(id);
        mapper.update(input, book);
        return repository.save(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = findOrFail(id);
        repository.delete(book);
    }

    private String generateBarcode(Long bookId, int sequence) {
        return String.format("BOOK-%d-COPY-%03d", bookId, sequence);
    }
}