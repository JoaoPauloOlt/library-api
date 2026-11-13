package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceLifecycleBehaviorTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-13T10:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void approveShouldMoveRequestedLoanToActiveAndLoanCopy() {
        Loan loan = new Loan();
        loan.setId(10L);
        loan.setStatus(LoanStatus.REQUESTED);

        BookCopy copy = new BookCopy();
        copy.setId(10L);
        copy.setStatus(CopyStatus.RESERVED);
        copy.setActive(true);
        loan.setBookCopy(copy);

        when(loanRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(bookCopyRepository.save(copy)).thenReturn(copy);

        Loan result = loanService.approve(10L);

        assertEquals(LoanStatus.ACTIVE, result.getStatus());
        assertEquals(CopyStatus.LOANED, copy.getStatus());
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(loan);
    }

    @Test
    void returnBookShouldAcceptLateLoanAndFreeCopy() {
        Loan loan = new Loan();
        loan.setId(11L);
        loan.setStatus(LoanStatus.LATE);

        BookCopy copy = new BookCopy();
        copy.setId(11L);
        copy.setStatus(CopyStatus.LOANED);
        copy.setActive(true);
        loan.setBookCopy(copy);

        when(loanRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(bookCopyRepository.save(copy)).thenReturn(copy);

        Loan result = loanService.returnBook(11L);

        assertEquals(LoanStatus.RETURNED, result.getStatus());
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(loan);
    }

    @Test
    void returnBookShouldMoveActiveLoanToReturnedAndFreeCopy() {
        Loan loan = new Loan();
        loan.setId(10L);
        loan.setStatus(LoanStatus.ACTIVE);

        BookCopy copy = new BookCopy();
        copy.setId(10L);
        copy.setStatus(CopyStatus.LOANED);
        copy.setActive(true);
        loan.setBookCopy(copy);

        when(loanRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(bookCopyRepository.save(copy)).thenReturn(copy);

        Loan result = loanService.returnBook(10L);

        assertEquals(LoanStatus.RETURNED, result.getStatus());
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(loan);
    }
}
