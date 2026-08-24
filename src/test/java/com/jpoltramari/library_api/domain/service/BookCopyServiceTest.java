package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyInput;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyUpdateInput;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookCopyServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository repository;

    private BookCopyService service;

    @BeforeEach
    void setUp() {
        service = new BookCopyService(bookRepository, repository);
    }

    @Test
    void shouldCreateAvailableActiveCopy() {
        Book book = new Book();
        book.setId(10L);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(repository.save(any(BookCopy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookCopy result = service.create(new BookCopyInput(10L, "A-01"));

        assertEquals(book, result.getBook());
        assertEquals(CopyStatus.AVAILABLE, result.getStatus());
        assertTrue(result.isActive());
        assertNotNull(result.getBarcode());
        assertTrue(result.getBarcode().startsWith("BK-"));
    }

    @Test
    void shouldRejectCreationWhenBookDoesNotExist() {
        when(bookRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class,
                () -> service.create(new BookCopyInput(10L, "A-01")));
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setLocation("A-01");
        copy.setActive(true);
        when(repository.findById(1L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy result = service.update(1L, new BookCopyUpdateInput(CopyStatus.MAINTENANCE, null, null));

        assertEquals(CopyStatus.MAINTENANCE, result.getStatus());
        assertEquals("A-01", result.getLocation());
        assertTrue(result.isActive());
    }

    @Test
    void shouldChangeStatus() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        when(repository.findById(1L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy result = service.changeStatus(1L, CopyStatus.MAINTENANCE);

        assertEquals(CopyStatus.MAINTENANCE, result.getStatus());
        verify(repository).save(copy);
    }

    @Test
    void shouldReturnTotalQuantity() {
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.countByBookId(10L)).thenReturn(5L);

        assertEquals(5L, service.totalQuantity(10L));
    }

    @Test
    void shouldReturnAvailableQuantity() {
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.countByBookIdAndStatus(10L, CopyStatus.AVAILABLE)).thenReturn(3L);

        assertEquals(3L, service.availableQuantity(10L));
    }

    @Test
    void shouldFindAllCopiesByBook() {
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findAllByBookId(10L)).thenReturn(List.of(new BookCopy(), new BookCopy()));

        assertEquals(2, service.findAllByBook(10L).size());
    }
}
