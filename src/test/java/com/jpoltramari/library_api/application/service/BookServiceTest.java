package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.book.CreateBookCommand;
import com.jpoltramari.library_api.application.command.book.UpdateBookCommand;
import com.jpoltramari.library_api.domain.enums.Genre;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository repository;
    @Mock private AuthorRepository authorRepository;
    @Mock private BookCopyRepository bookCopyRepository;

    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(repository, authorRepository, bookCopyRepository);
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
    void shouldCreateBookWithAuthorsAndPhysicalCopies() {
        CreateBookCommand command = new CreateBookCommand(
                "9781234567890", "Clean Code", Genre.COMIC, null, null, 3, List.of(1L));
        Author author = new Author();

        when(repository.existsByIsbn(command.isbn())).thenReturn(false);
        when(authorRepository.findAllById(List.of(1L))).thenReturn(List.of(author));
        when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = service.create(command);

        assertEquals(Set.of(author), result.getAuthors());
        assertEquals(command.isbn(), result.getIsbn());
        assertEquals(command.title(), result.getTitle());
        verify(repository).save(any(Book.class));
        verify(bookCopyRepository).saveAll(any(List.class));
    }

    @Test
    void shouldCreateBookWithoutPhysicalCopiesWhenQuantityIsZero() {
        CreateBookCommand command = new CreateBookCommand(
                "9781234567890", "Clean Code", Genre.COMIC, null, null, 0, List.of(1L));
        Author author = new Author();

        when(repository.existsByIsbn(command.isbn())).thenReturn(false);
        when(authorRepository.findAllById(List.of(1L))).thenReturn(List.of(author));
        when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(command);

        verify(bookCopyRepository, never()).saveAll(any(List.class));
    }

    @Test
    void shouldRejectDuplicateIsbn() {
        CreateBookCommand command = new CreateBookCommand(
                "9781234567890", "Clean Code", Genre.COMIC, null, null, 0, List.of(1L));
        when(repository.existsByIsbn(command.isbn())).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(command));
    }

    @Test
    void shouldRejectMissingAuthor() {
        CreateBookCommand command = new CreateBookCommand(
                "9781234567890", "Clean Code", Genre.COMIC, null, null, 0, List.of(1L, 2L));
        when(repository.existsByIsbn(command.isbn())).thenReturn(false);
        when(authorRepository.findAllById(command.authorIds())).thenReturn(List.of(new Author()));

        assertThrows(EntityNotFoundException.class, () -> service.create(command));
    }

    @Test
    void shouldUpdateBookFromCommand() {
        Book book = new Book();
        book.setIsbn("9781234567890");
        when(repository.findById(1L)).thenReturn(Optional.of(book));
        when(repository.save(book)).thenReturn(book);

        UpdateBookCommand command = new UpdateBookCommand(
                null, "Clean Code 2", Genre.COMIC, "Updated", "https://example.com/cover.jpg", null);

        Book result = service.update(1L, command);

        assertEquals("Clean Code 2", result.getTitle());
        assertEquals("Updated", result.getDescription());
        assertEquals("https://example.com/cover.jpg", result.getCoverUrl());
        verify(repository).save(book);
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
