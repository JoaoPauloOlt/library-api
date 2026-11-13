package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.bookcopy.CreateBookCopyCommand;
import com.jpoltramari.library_api.application.command.bookcopy.UpdateBookCopyCommand;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.exception.BusinessException;
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
import static org.mockito.Mockito.never;
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

        BookCopy result = service.create(10L, new CreateBookCopyCommand(10L, "A-01"));

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
                () -> service.create(10L, new CreateBookCopyCommand(10L, "A-01")));
    }


    @Test
    void shouldRejectCreationWhenPathBookDiffersFromCommand() {
        assertThrows(com.jpoltramari.library_api.domain.exception.BusinessException.class,
                () -> service.create(10L, new CreateBookCopyCommand(20L, "A-01")));
    }

    @Test
    void shouldUpdateAllProvidedFields() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setLocation("A-01");
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy result = service.update(10L, 1L,
                new UpdateBookCopyCommand(CopyStatus.MAINTENANCE, "B-02", false));

        assertEquals(CopyStatus.MAINTENANCE, result.getStatus());
        assertEquals("B-02", result.getLocation());
        assertFalse(result.isActive());
    }

    @Test
    void shouldRejectCopyFromAnotherBook() {
        BookCopy copy = new BookCopy();
        Book book = new Book();
        book.setId(20L);
        copy.setBook(book);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookId(1L, 10L)).thenReturn(Optional.empty());

        assertThrows(com.jpoltramari.library_api.domain.exception.EntityNotFoundException.class,
                () -> service.findOrFailForBook(10L, 1L));
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setLocation("A-01");
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy result = service.update(10L, 1L, new UpdateBookCopyCommand(CopyStatus.MAINTENANCE, null, null));

        assertEquals(CopyStatus.MAINTENANCE, result.getStatus());
        assertEquals("A-01", result.getLocation());
        assertTrue(result.isActive());
    }

    @Test
    void shouldChangeStatus() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy result = service.changeStatus(10L, 1L, CopyStatus.MAINTENANCE);

        assertEquals(CopyStatus.MAINTENANCE, result.getStatus());
        verify(repository).save(copy);
    }

    @Test
    void shouldRejectManualStatusChangeOfLoanedCopy() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.LOANED);
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class,
                () -> service.changeStatus(10L, 1L, CopyStatus.MAINTENANCE));
        verify(repository, never()).save(copy);
    }

    @Test
    void shouldRejectDeactivatingLoanedCopy() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.LOANED);
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class,
                () -> service.update(10L, 1L, new UpdateBookCopyCommand(null, null, false)));
        verify(repository, never()).save(copy);
    }

    @Test
    void shouldRejectDeletingReservedAndLoanedCopies() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.RESERVED);
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class, () -> service.delete(10L, 1L));

        copy.setStatus(CopyStatus.LOANED);
        assertThrows(BusinessException.class, () -> service.delete(10L, 1L));
        verify(repository, never()).delete(copy);
    }

    @Test
    void shouldDeleteAvailableCopy() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));

        service.delete(10L, 1L);

        verify(repository).delete(copy);
    }

    @Test
    void shouldCoverCommandAccessors() {
        CreateBookCopyCommand create = new CreateBookCopyCommand(10L, "A-01");
        UpdateBookCopyCommand update = new UpdateBookCopyCommand(CopyStatus.MAINTENANCE, "B-02", true);

        assertEquals(10L, create.bookId());
        assertEquals("A-01", create.location());
        assertEquals(CopyStatus.MAINTENANCE, update.status());
        assertEquals("B-02", update.location());
        assertTrue(update.active());
    }

    @Test
    void shouldMarkLostCopyInactiveWhenChangingStatus() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy result = service.changeStatus(10L, 1L, CopyStatus.LOST);

        assertEquals(CopyStatus.LOST, result.getStatus());
        assertFalse(result.isActive());
    }

    @Test
    void shouldMarkDiscardedCopyInactiveWhenChangingStatus() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy result = service.changeStatus(10L, 1L, CopyStatus.DISCARDED);

        assertEquals(CopyStatus.DISCARDED, result.getStatus());
        assertFalse(result.isActive());
    }

    @Test
    void shouldRejectActivatingLostCopyWithoutRestoringItsStatus() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.LOST);
        copy.setActive(false);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class,
                () -> service.update(10L, 1L, new UpdateBookCopyCommand(null, null, true)));
        verify(repository, never()).save(copy);
    }

    @Test
    void shouldAllowRestoringLostCopyAsAvailableAndActive() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.LOST);
        copy.setActive(false);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy result = service.update(
                10L, 1L, new UpdateBookCopyCommand(CopyStatus.AVAILABLE, null, true)
        );

        assertEquals(CopyStatus.AVAILABLE, result.getStatus());
        assertTrue(result.isActive());
    }

    @Test
    void shouldRejectReturningDiscardedCopyToCirculation() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.DISCARDED);
        copy.setActive(false);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class,
                () -> service.update(
                        10L, 1L, new UpdateBookCopyCommand(CopyStatus.AVAILABLE, null, true)
                ));
        verify(repository, never()).save(copy);
    }

    @Test
    void shouldRejectSettingRetiredStatusAsActiveInOneUpdate() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class,
                () -> service.update(
                        10L, 1L, new UpdateBookCopyCommand(CopyStatus.LOST, null, true)
                ));
        verify(repository, never()).save(copy);
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
        when(repository.countByBookIdAndStatusAndActiveTrue(10L, CopyStatus.AVAILABLE)).thenReturn(3L);

        assertEquals(3L, service.availableQuantity(10L));
    }

    @Test
    void shouldFindAllCopiesByBook() {
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(repository.findAllByBookId(10L)).thenReturn(List.of(new BookCopy(), new BookCopy()));

        assertEquals(2, service.findAllByBook(10L).size());
    }
}
