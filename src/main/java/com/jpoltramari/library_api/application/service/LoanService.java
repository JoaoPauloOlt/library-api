package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.LoanNotFoundException;
import com.jpoltramari.library_api.domain.exception.UserNotFoundException;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final int LOAN_DAYS = 7;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;

    public Page<Loan> findAll(Pageable pageable) {
        return loanRepository.findAll(pageable);
    }

    public Page<Loan> findByUserId(Long userId, Pageable pageable) {
        return loanRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public Loan create(CreateLoanCommand command, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Long bookId = command.bookId();

        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }

        validateNoActiveLoan(user, bookId);

        BookCopy copy = bookCopyRepository.findFirstByBookIdAndActiveTrueAndStatusOrderByIdAsc(
                        bookId, CopyStatus.AVAILABLE)
                .orElseThrow(() -> new BusinessException("No available copies for this book."));

        if (copy.getStatus() != CopyStatus.AVAILABLE || !copy.isActive()) {
            throw new BusinessException("Copy no longer available.");
        }

        // Reserve the physical copy atomically with the loan request.
        copy.setStatus(CopyStatus.RESERVED);
        bookCopyRepository.save(copy);

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBookCopy(copy);
        loan.setStatus(LoanStatus.REQUESTED);
        loan.setRequestDate(LocalDateTime.now(ZoneOffset.UTC));

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan approve(Long id) {
        Loan loan = findForUpdateOrFail(id);
        validateStatus(loan, LoanStatus.REQUESTED);

        // Lock the physical copy as well: distinct legacy loan rows can reference the same copy.
        BookCopy copy = findCopyForUpdate(loan);
        if (!copy.isActive()) {
            throw new BusinessException("The copy is inactive.");
        }
        if (copy.getStatus() != CopyStatus.RESERVED) {
            throw new BusinessException("Copy is not reserved for this loan.");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        copy.setStatus(CopyStatus.LOANED);
        bookCopyRepository.save(copy);

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setApprovalDate(now);
        loan.setWithdrawableDate(now);
        loan.setDueDate(now.plusDays(LOAN_DAYS));

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan returnBook(Long id) {
        Loan loan = findOrFail(id);
        validateStatus(loan, LoanStatus.ACTIVE);

        BookCopy copy = loan.getBookCopy();
        if (copy.getStatus() != CopyStatus.LOANED) {
            throw new BusinessException("Copy is not currently loaned");
        }

        copy.setStatus(CopyStatus.AVAILABLE);
        bookCopyRepository.save(copy);

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDateTime.now(ZoneOffset.UTC));

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan cancel(Long id, AuthenticatedPrincipal authenticatedPrincipal) {
        Loan loan = findOrFail(id);
        if (!authenticatedPrincipal.hasPermission("LOAN_CANCEL_ANY")) {
            validateLoanOwnership(loan, id, authenticatedPrincipal.getUserId());
        }

        if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Only requested loans can be cancelled.");
        }

        loan.setStatus(LoanStatus.CANCELED);
        return loanRepository.save(loan);
    }

    public Loan findOrFail(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new LoanNotFoundException(id));
    }

    private Loan findForUpdateOrFail(Long id) {
        return loanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new LoanNotFoundException(id));
    }

    private BookCopy findCopyForUpdate(Loan loan) {
        if (loan.getBookCopy() == null || loan.getBookCopy().getId() == null) {
            throw new BusinessException("The copy associated with this loan could not be found.");
        }

        Long copyId = loan.getBookCopy().getId();
        return bookCopyRepository.findByIdForUpdate(copyId)
                .orElseThrow(() -> new BusinessException("The copy associated with this loan could not be found."));
    }

    private void validateLoanOwnership(Loan loan, Long loanId, Long authenticatedUserId) {
        if (loan.getUser() == null || !Objects.equals(loan.getUser().getId(), authenticatedUserId)) {
            // Return the same response as a missing loan to avoid exposing another user's resources.
            throw new LoanNotFoundException(loanId);
        }
    }

    private void validateStatus(Loan loan, LoanStatus expectedStatus) {
        if (loan.getStatus() != expectedStatus) {
            throw new BusinessException(
                    String.format("Invalid status transition. Expected: %s, current: %s",
                            expectedStatus, loan.getStatus())
            );
        }
    }

    private void validateNoActiveLoan(User user, Long bookId) {
        boolean exists = loanRepository.existsByUserIdAndBookCopyBookIdAndStatusIn(
                user.getId(), bookId,
                List.of(LoanStatus.REQUESTED, LoanStatus.ACTIVE, LoanStatus.LATE));

        if (exists) {
            throw new BusinessException("User already has an active loan for this book.");
        }
    }
}
