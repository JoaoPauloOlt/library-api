package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.UserNotFoundException;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceForUserTest {

    @Mock private LoanRepository loanRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private UserRepository userRepository;

    private LoanService service;

    @BeforeEach
    void setUp() {
        service = new LoanService(
                loanRepository,
                bookRepository,
                bookCopyRepository,
                userRepository,
                Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateLoanForActiveBorrowerAndReserveAvailableCopy() {
        User borrower = activeBorrower();
        BookCopy copy = new BookCopy();
        copy.setId(15L);
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);

        when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(borrower));
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(loanRepository.existsByUserIdAndBookCopyBookIdAndStatusIn(42L, 10L,
                List.of(LoanStatus.REQUESTED, LoanStatus.ACTIVE, LoanStatus.LATE))).thenReturn(false);
        when(bookCopyRepository.findFirstByBookIdAndActiveTrueAndStatusOrderByIdAsc(10L, CopyStatus.AVAILABLE))
                .thenReturn(Optional.of(copy));
        when(bookCopyRepository.save(copy)).thenReturn(copy);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = service.createForUser(new CreateLoanCommand(10L), 42L);

        assertEquals(LoanStatus.REQUESTED, result.getStatus());
        assertEquals(borrower, result.getUser());
        assertEquals(CopyStatus.RESERVED, copy.getStatus());
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(result);
    }

    @Test
    void shouldRejectInactiveBorrowerBeforeCheckingBookAvailability() {
        User borrower = activeBorrower();
        borrower.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(borrower));

        assertThrows(BusinessException.class,
                () -> service.createForUser(new CreateLoanCommand(10L), 42L));

        verify(bookRepository, never()).existsById(any());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldReturnNotFoundWhenBorrowerDoesNotExist() {
        when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.createForUser(new CreateLoanCommand(10L), 42L));

        verify(bookRepository, never()).existsById(any());
    }

    @Test
    void shouldReturnNotFoundWhenBookDoesNotExist() {
        when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(activeBorrower()));
        when(bookRepository.existsById(10L)).thenReturn(false);

        assertThrows(BookNotFoundException.class,
                () -> service.createForUser(new CreateLoanCommand(10L), 42L));

        verify(loanRepository, never()).save(any(Loan.class));
    }

    private User activeBorrower() {
        User user = new User();
        user.setId(42L);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
