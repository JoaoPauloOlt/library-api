package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.web.dto.loan.LoanCreateInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private UserRepository userRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, bookCopyRepository, userRepository);
    }

    @Test
    void shouldCreateLoanWhenUserAndCopyAreAvailable() {
        User user = new User();
        BookCopy copy = new BookCopy();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookCopyRepository.findFirstAvailableCopy(1L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanCreateInput input = new LoanCreateInput(1L);

        Loan result = loanService.create(1L, input);

        assertNotNull(result);
    }
}
