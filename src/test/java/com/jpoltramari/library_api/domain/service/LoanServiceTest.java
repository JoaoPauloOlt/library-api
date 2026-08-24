package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.loan.LoanInput;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
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
import static org.mockito.ArgumentMatchers.any;
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

        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);

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
    }
}
