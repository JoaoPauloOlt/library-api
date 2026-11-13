package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceValidationTest {

    @Mock private LoanRepository loanRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private UserRepository userRepository;

    private LoanService service;

    @BeforeEach
    void setUp() {
        service = new LoanService(loanRepository, bookRepository, bookCopyRepository, userRepository);
    }

    @Test
    void shouldRejectApprovalWhenLoanIsNotRequested() {
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> service.approve(1L));
    }

    @Test
    void shouldRejectReturnWhenLoanIsNotActive() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> service.returnBook(1L));
    }

    @Test
    void shouldRejectCancellationWhenLoanIsActive() {
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> service.cancel(1L, principal(42L, "LOAN_CANCEL")));
    }

    @Test
    void shouldRejectCancellationWhenLoanIsReturned() {
        Loan loan = loanWithStatus(LoanStatus.RETURNED);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> service.cancel(1L, principal(42L, "LOAN_CANCEL")));
    }

    private AuthenticatedPrincipal principal(Long userId, String... permissions) {
        List<String> grantedPermissions = List.of(permissions);
        return new AuthenticatedPrincipal() {
            @Override public Long getUserId() { return userId; }
            @Override public boolean hasPermission(String permission) { return grantedPermissions.contains(permission); }
        };
    }

    private Loan loanWithStatus(LoanStatus status) {
        Loan loan = new Loan();
        loan.setStatus(status);
        loan.setBookCopy(new BookCopy());
        User owner = new User();
        owner.setId(42L);
        loan.setUser(owner);
        return loan;
    }
}
