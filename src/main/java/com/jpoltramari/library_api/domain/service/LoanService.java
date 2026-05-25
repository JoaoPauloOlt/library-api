package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.loan.LoanInput;
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
import java.util.List;

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
    public Loan create(LoanInput input, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Long bookId = input.bookId();

        if (!bookRepository.existsById(bookId)){
            throw new BookNotFoundException(bookId);
        }

        validateNoActiveLoan(user, bookId);

        BookCopy copy = bookCopyRepository
                .findFirstAvailableCopy(bookId)
                .orElseThrow(() ->
                        new BusinessException("No available copies for this book."));

        if (copy.getStatus() != CopyStatus.AVAILABLE || !copy.isActive()){
            throw new BusinessException("Copy no longer available.");
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBookCopy(copy);
        loan.setStatus(LoanStatus.REQUESTED);
        loan.setRequestDate(LocalDateTime.now());

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan approve(Long id) {
        Loan loan = findOrFail(id);

        validateStatus(loan, LoanStatus.REQUESTED);

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDateTime.now());

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan withdraw(Long id) {
        Loan loan = findOrFail(id);

        validateStatus(loan, LoanStatus.APPROVED);

        BookCopy copy = loan.getBookCopy();

        if (!copy.isActive()){
            throw new BusinessException("The copy is inactive");
        }

        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new BusinessException("Copy is not available.");
        }

        copy.setStatus(CopyStatus.LOANED);
        bookCopyRepository.save(copy);

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setWithdrawableDate(LocalDateTime.now());
        loan.setDueDate(LocalDateTime.now().plusDays(LOAN_DAYS));

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan returnBook(Long id) {
        Loan loan = findOrFail(id);

        validateStatus(loan, LoanStatus.ACTIVE);

        BookCopy copy = loan.getBookCopy();

        if (copy.getStatus() != CopyStatus.LOANED){
            throw new BusinessException("Copy is not currently loaned");
        }

        copy.setStatus(CopyStatus.AVAILABLE);
        bookCopyRepository.save(copy);

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDateTime.now());

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan cancel(Long id) {
        Loan loan = findOrFail(id);

        if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Only requested or approved loans can be cancelled.");
        }

        loan.setStatus(LoanStatus.CANCELED);

        return loanRepository.save(loan);
    }

    public Loan findOrFail(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new LoanNotFoundException(id));
    }

    private void validateStatus(Loan loan, LoanStatus expectedStatus) {
        if (loan.getStatus() != expectedStatus) {
            throw new BusinessException(
                    String.format("Invalid status transition. Expected: %s, current: %s",
                            expectedStatus,
                            loan.getStatus()
                    )
            );
        }
    }

    private void validateNoActiveLoan(User user, Long bookId){
        boolean exists = loanRepository.existsByUserIdAndBookCopyBookIdAndStatusIn(
                user.getId(),
                bookId,
                List.of(
                        LoanStatus.REQUESTED,
                        LoanStatus.APPROVED,
                        LoanStatus.ACTIVE,
                        LoanStatus.LATE
                )
        );

        if (exists){
            throw new BusinessException("User already has an active loan for this book.");
        }
    }
}