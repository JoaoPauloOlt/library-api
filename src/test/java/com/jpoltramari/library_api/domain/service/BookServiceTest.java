package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.book.BookInput;
import com.jpoltramari.library_api.api.mapper.BookMapper;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.repository.AuthorRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository repository;
    @Mock private AuthorRepository authorRepository;
    @Mock private BookMapper mapper;

    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(repository, authorRepository, mapper);
    }

    @Test
    void shouldFindBookOrFail() {
        Book book = new Book();
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        assertEquals(book, service.findOrFail(1L));
    }

    @Test
    void shouldThrowWhenBookDoesNotExist() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> service.findOrFail(1L));
    }

    @Test
    void shouldCreateBookWithAuthors() {
        BookInput input = new BookInput("9781234567890", "Clean Code", Genre.COMIC, null, List.of(1L));
        Author author = new Author();
        Book book = new Book();

        when(repository.existsByIsbn(input.isbn())).thenReturn(false);
        when(authorRepository.findAllById(List.of(1L))).thenReturn(List.of(author));
        when(mapper.toEntity(input)).thenReturn(book);
        when(repository.save(book)).thenReturn(book);

        Book result = service.create(input);

        assertEquals(Set.of(author), result.getAuthors());
        verify(repository).save(book);
    }

    @Test
    void shouldRejectDuplicateIsbn() {
        BookInput input = new BookInput("9781234567890", "Clean Code", Genre.COMIC, null, List.of(1L));
        when(repository.existsByIsbn(input.isbn())).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(input));
    }

    @Test
    void shouldRejectMissingAuthor() {
        BookInput input = new BookInput("9781234567890", "Clean Code", Genre.COMIC, null, List.of(1L, 2L));
        when(repository.existsByIsbn(input.isbn())).thenReturn(false);
        when(authorRepository.findAllById(input.authorIds())).thenReturn(List.of(new Author()));

        assertThrows(EntityNotFoundException.class, () -> service.create(input));
    }

    @Test
    void shouldRejectDeletingBookWithCopies() {
        Book book = new Book();
        book.setCopies(Set.of(new BookCopy()));
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        assertThrows(BusinessException.class, () -> service.delete(1L));
    }

    @Test
    void shouldDeleteBookWithoutCopies() {
        Book book = new Book();
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        service.delete(1L);

        verify(repository).delete(book);
    }

    @Test
    void shouldReturnFilteredPage() {
        BookFilter filter = new BookFilter();
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new Book()), pageable, 1));

        assertNotNull(service.findAll(filter, pageable));
    }
}
