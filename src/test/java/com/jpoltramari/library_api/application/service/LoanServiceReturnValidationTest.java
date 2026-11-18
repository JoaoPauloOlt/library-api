package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.LoanNotFoundException;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceReturnValidationTest {

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
    void shouldRejectASecondReturnOfAnAlreadyReturnedLoan() {
        Loan returnedLoan = loan(LoanStatus.RETURNED);
        when(loanRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(returnedLoan));

        assertThrows(BusinessException.class, () -> service.returnBook(10L));

        verify(bookCopyRepository, never()).findByIdForUpdate(any());
        verify(bookCopyRepository, never()).save(any(BookCopy.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldReturnNotFoundWhenLoanDoesNotExist() {
        when(loanRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(LoanNotFoundException.class, () -> service.returnBook(10L));

        verify(bookCopyRepository, never()).findByIdForUpdate(any());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldRejectReturnWhenTheAssociatedCopyIsNotLoaned() {
        Loan activeLoan = loan(LoanStatus.ACTIVE);
        BookCopy copy = new BookCopy();
        copy.setId(15L);
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        activeLoan.setBookCopy(copy);

        when(loanRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(activeLoan));
        when(bookCopyRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class, () -> service.returnBook(10L));

        verify(bookCopyRepository, never()).save(any(BookCopy.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    private Loan loan(LoanStatus status) {
        Loan loan = new Loan();
        loan.setId(10L);
        loan.setStatus(status);
        return loan;
    }
}
