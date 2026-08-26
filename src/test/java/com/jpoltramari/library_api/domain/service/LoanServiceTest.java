package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.loan.LoanInput;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private UserRepository userRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(
                loanRepository,
                bookRepository,
                bookCopyRepository,
                userRepository
        );
    }

    @Test
    void shouldCreateRequestedLoanWhenUserAndCopyAreAvailable() {
        User user = new User();
        user.setId(1L);

        BookCopy copy = availableCopy();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(loanRepository.existsByUserIdAndBookCopyBookIdAndStatusIn(any(), any(), any()))
                .thenReturn(false);
        when(bookCopyRepository.findFirstAvailableCopy(10L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(any(Loan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.create(new LoanInput(10L), 1L);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(copy, result.getBookCopy());
        assertEquals(LoanStatus.REQUESTED, result.getStatus());
        assertNotNull(result.getRequestDate());
    }

    @Test
    void shouldApproveRequestedLoanAndActivateItImmediately() {
        BookCopy copy = availableCopy();
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        loan.setBookCopy(copy);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(bookCopyRepository.save(copy)).thenReturn(copy);

        Loan result = loanService.approve(1L);

        assertEquals(LoanStatus.ACTIVE, result.getStatus());
        assertEquals(CopyStatus.LOANED, copy.getStatus());
        assertNotNull(result.getApprovalDate());
        assertNotNull(result.getWithdrawableDate());
        assertNotNull(result.getDueDate());
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(loan);
    }

    @Test
    void shouldRejectApprovalWhenLoanIsNotRequested() {
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> loanService.approve(1L));
    }

    @Test
    void shouldRejectLegacyWithdrawTransition() {
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> loanService.withdraw(1L));
    }

    @Test
    void shouldReturnActiveLoanAndMakeCopyAvailable() {
        BookCopy copy = availableCopy();
        copy.setStatus(CopyStatus.LOANED);
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        loan.setBookCopy(copy);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(bookCopyRepository.save(copy)).thenReturn(copy);

        Loan result = loanService.returnBook(1L);

        assertEquals(LoanStatus.RETURNED, result.getStatus());
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        assertNotNull(result.getReturnDate());
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(loan);
    }

    @Test
    void shouldCancelRequestedLoan() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan result = loanService.cancel(1L);

        assertEquals(LoanStatus.CANCELED, result.getStatus());
        verify(loanRepository).save(loan);
    }

    @Test
    void shouldRejectCancellationOfActiveLoan() {
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> loanService.cancel(1L));
    }

    private Loan loanWithStatus(LoanStatus status) {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setStatus(status);
        return loan;
    }

    private BookCopy availableCopy() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        return copy;
    }
}
