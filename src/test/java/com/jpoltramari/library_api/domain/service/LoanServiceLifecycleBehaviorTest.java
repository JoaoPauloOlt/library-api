package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private LoanService loanService;

    @Test
    void approveShouldMoveRequestedLoanToActiveAndLoanCopy() {
        Loan loan = new Loan();
        loan.setId(10L);
        loan.setStatus(LoanStatus.REQUESTED);

        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        loan.setBookCopy(copy);

        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(bookCopyRepository.save(copy)).thenReturn(copy);

        Loan result = loanService.approve(10L);

        assertEquals(LoanStatus.ACTIVE, result.getStatus());
        assertEquals(CopyStatus.LOANED, copy.getStatus());
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(loan);
    }

    @Test
    void returnBookShouldMoveActiveLoanToReturnedAndFreeCopy() {
        Loan loan = new Loan();
        loan.setId(10L);
        loan.setStatus(LoanStatus.ACTIVE);

        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.LOANED);
        copy.setActive(true);
        loan.setBookCopy(copy);

        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(bookCopyRepository.save(copy)).thenReturn(copy);

        Loan result = loanService.returnBook(10L);

        assertEquals(LoanStatus.RETURNED, result.getStatus());
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(loan);
    }
}
