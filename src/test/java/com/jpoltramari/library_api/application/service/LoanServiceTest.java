package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.LoanNotFoundException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private UserRepository userRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, bookRepository, bookCopyRepository, userRepository);
    }

    @Test
    void shouldCreateRequestedLoanAndReserveSelectedCopy() {
        User user = new User();
        user.setId(1L);
        BookCopy copy = availableCopy();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(loanRepository.existsByUserIdAndBookCopyBookIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(bookCopyRepository.findFirstByBookIdAndActiveTrueAndStatusOrderByIdAsc(10L, CopyStatus.AVAILABLE))
                .thenReturn(Optional.of(copy));
        when(bookCopyRepository.save(copy)).thenReturn(copy);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.create(new CreateLoanCommand(10L), 1L);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(copy, result.getBookCopy());
        assertEquals(LoanStatus.REQUESTED, result.getStatus());
        assertEquals(CopyStatus.RESERVED, copy.getStatus());
        assertNotNull(result.getRequestDate());
        verify(bookCopyRepository).save(copy);
    }

    @Test
    void shouldApproveRequestedLoanAndActivateItImmediately() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        BookCopy copy = loan.getBookCopy();

        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(bookCopyRepository.save(copy)).thenReturn(copy);

        Loan result = loanService.approve(1L);

        assertEquals(LoanStatus.ACTIVE, result.getStatus());
        assertEquals(CopyStatus.LOANED, copy.getStatus());
        assertNotNull(result.getApprovalDate());
        assertNotNull(result.getWithdrawableDate());
        assertNotNull(result.getDueDate());
        verify(bookCopyRepository).findByIdForUpdate(10L);
        verify(bookCopyRepository).save(copy);
        verify(loanRepository).save(loan);
    }

    @Test
    void shouldRejectApprovalWhenCopyIsNotReserved() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        BookCopy copy = loan.getBookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class, () -> loanService.approve(1L));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldRejectApprovalWhenLoanIsNotRequested() {
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> loanService.approve(1L));
        verify(bookCopyRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldReturnActiveLoanAndMakeCopyAvailable() {
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        BookCopy copy = loan.getBookCopy();
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
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
    void shouldCancelRequestedLoanOwnedByAuthenticatedUserAndReleaseCopy() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        BookCopy copy = loan.getBookCopy();
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
        when(bookCopyRepository.save(copy)).thenReturn(copy);
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan result = loanService.cancel(1L, principal(42L, "LOAN_CANCEL"));

        assertEquals(LoanStatus.CANCELED, result.getStatus());
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        verify(bookCopyRepository).save(copy);
    }

    @Test
    void shouldNotExposeOrCancelLoanOwnedByAnotherUser() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        User otherOwner = new User();
        otherOwner.setId(99L);
        loan.setUser(otherOwner);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(LoanNotFoundException.class,
                () -> loanService.cancel(1L, principal(42L, "LOAN_CANCEL")));
        verify(bookCopyRepository, never()).findByIdForUpdate(any());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldAllowStaffWithCancelAnyPermissionToCancelAnotherUsersLoan() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        User otherOwner = new User();
        otherOwner.setId(99L);
        loan.setUser(otherOwner);
        BookCopy copy = loan.getBookCopy();
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
        when(bookCopyRepository.save(copy)).thenReturn(copy);
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan result = loanService.cancel(1L, principal(42L, "LOAN_CANCEL_ANY"));

        assertEquals(LoanStatus.CANCELED, result.getStatus());
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void shouldCancelStaleRequestWithoutReleasingCopyAlreadyLoanedByAnotherLoan() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        BookCopy copy = loan.getBookCopy();
        copy.setStatus(CopyStatus.LOANED);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan result = loanService.cancel(1L, principal(42L, "LOAN_CANCEL"));

        assertEquals(LoanStatus.CANCELED, result.getStatus());
        assertEquals(CopyStatus.LOANED, copy.getStatus());
        verify(bookCopyRepository, never()).save(any(BookCopy.class));
    }

    @Test
    void shouldThrowNotFoundWhenApprovalLoanDoesNotExist() {
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(LoanNotFoundException.class, () -> loanService.approve(1L));
    }

    @Test
    void shouldRejectApprovalWhenLoanHasNoAssociatedCopy() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        loan.setBookCopy(null);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> loanService.approve(1L));
        verify(bookCopyRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldRejectApprovalWhenAssociatedCopyHasNoId() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.RESERVED);
        copy.setActive(true);
        loan.setBookCopy(copy);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> loanService.approve(1L));
        verify(bookCopyRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldRejectApprovalWhenAssociatedCopyDoesNotExist() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> loanService.approve(1L));
    }

    @Test
    void shouldRejectApprovalWhenCopyIsInactive() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        BookCopy copy = loan.getBookCopy();
        copy.setActive(false);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class, () -> loanService.approve(1L));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldCancelLegacyRequestWhenCopyIsAlreadyAvailable() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        BookCopy copy = loan.getBookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan result = loanService.cancel(1L, principal(42L, "LOAN_CANCEL"));

        assertEquals(LoanStatus.CANCELED, result.getStatus());
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        verify(bookCopyRepository, never()).save(any(BookCopy.class));
    }

    @Test
    void shouldRejectCancellationWhenCopyCannotBeReleased() {
        Loan loan = loanWithStatus(LoanStatus.REQUESTED);
        BookCopy copy = loan.getBookCopy();
        copy.setStatus(CopyStatus.MAINTENANCE);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));

        assertThrows(BusinessException.class,
                () -> loanService.cancel(1L, principal(42L, "LOAN_CANCEL")));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldRejectCancellationOfActiveLoan() {
        Loan loan = loanWithStatus(LoanStatus.ACTIVE);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessException.class, () -> loanService.cancel(1L, principal(42L, "LOAN_CANCEL")));
        verify(bookCopyRepository, never()).findByIdForUpdate(any());
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
        loan.setId(1L);
        loan.setStatus(status);
        User owner = new User();
        owner.setId(42L);
        loan.setUser(owner);
        BookCopy copy = availableCopy();
        copy.setId(10L);
        copy.setStatus(status == LoanStatus.REQUESTED ? CopyStatus.RESERVED
                : status == LoanStatus.ACTIVE || status == LoanStatus.LATE ? CopyStatus.LOANED
                : CopyStatus.AVAILABLE);
        loan.setBookCopy(copy);
        return loan;
    }

    private BookCopy availableCopy() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        return copy;
    }
}
