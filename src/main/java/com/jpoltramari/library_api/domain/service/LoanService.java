package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.LoanInput;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.LoanNotFoundException;
import com.jpoltramari.library_api.domain.exception.UserNotFoundException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoanService{

    private static final int DEFAULT_LOAN_DAYS = 7;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;

    public LoanService(LoanRepository loanRepository,
                       BookRepository bookRepository,
                       BookCopyRepository bookCopyRepository,
                       UserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.userRepository = userRepository;
    }

    public Page<Loan> findAll(Pageable pageable) {
        return loanRepository.findAll(pageable);
    }

    @Transactional
    public Loan create(LoanInput input, User user) {

        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException(user.getId()));

        Book book = bookRepository.findById(input.getBookId())
                .orElseThrow(() ->
                        new BookNotFoundException(input.getBookId()));

        BookCopy copy = bookCopyRepository
                .findFirstAvailableCopy(book.getId())
                .orElseThrow(() ->
                        new BusinessException("No available copies"));

        Loan loan = new Loan();
        loan.setUser(managedUser);
        loan.setBookCopy(copy);
        loan.setStatus(LoanStatus.REQUESTED);
        loan.setRequestDate(LocalDateTime.now());

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan approve(Long id) {
        Loan loan = findOrFail(id);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    @Transactional
    public Loan withdraw(Long id) {
        Loan loan = findOrFail(id);

        BookCopy copy = loan.getBookCopy();
        copy.setStatus(CopyStatus.LOANED);

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setWithdrawableDate(LocalDateTime.now());
        loan.setDueDate(LocalDateTime.now().plusDays(DEFAULT_LOAN_DAYS));

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan returnBook(Long id) {
        Loan loan = findOrFail(id);

        BookCopy copy = loan.getBookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDateTime.now());

        return loanRepository.save(loan);
    }

    private Loan findOrFail(Long id){
        return loanRepository.findById(id)
                .orElseThrow(()-> new LoanNotFoundException(id));
    }
}