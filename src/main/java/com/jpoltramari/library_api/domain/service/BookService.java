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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final String DEFAULT_LOCATION = "Shelf A";

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

        validateIsbn(input.getIsbn());

        Book book = mapper.toEntity(input);

        Set<Author> authors = load.findAllById(input.getAuthorIds());
        book.setAuthors(authors);

        Book saved = repository.save(book);

        createCopies(saved, input.getTotalQuantity());
        return saved;
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

    private void validateIsbn(String isbn) {
        if (repository.existsByIsbn(isbn)) {
            throw new BusinessException("ISBN already registered");
        }
    }

    private Set<Author> loadAuthors(List<Long> authorIds) {
        List<Author> authors = authorRepository.findAllById(authorIds);
        if(authors.size() != new HashSet<>(ids).size()){
            throw new EntityNotFoundException("One or more authors were not found");
        }
        return new HashSet<>(authors);
    }

    private void createCopies(Book book, int quantity){
        for (int i = 1; i <= quantity; i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setStatus(CopyStatus.AVAILABLE);
            copy.setLocation("DEFAULT-SHELF");
            copy.setBarcode(generateBarcode(book.getId(), i));

            copyRepository.save(copy);
        }
    }

    private String generateBarcode(Long bookId, int index) {
        return String.format("BK-%d-%04d", bookId, index);
    }
}