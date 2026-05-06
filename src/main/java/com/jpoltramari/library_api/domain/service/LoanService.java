package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.LoanInput;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoanService {

    @Autowired
    private LoanRepository repository;

    @Autowired
    private BookRepository bookRepository;

    public Page<Loan> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    public Loan create(LoanInput input, User authenticatedUser) {

        Book book = bookRepository.findById(input.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));

        if (book.getAvailableQuantity() <= 0) {
            throw new BusinessException("Book unavailable");
        }

        long activeLoans = repository.countActiveLoansByUser(authenticatedUser.getId());

        if (activeLoans >= 3) {
            throw new BusinessException("User has reached active loan limit");
        }

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setUser(authenticatedUser);
        loan.setStatus("SOLICITADO");
        loan.setRequestDate(LocalDateTime.now());

        return repository.save(loan);
    }

    public Loan approve(Long id) {
        Loan loan = findOrFail(id);

        if (!loan.getStatus().equals("SOLICITADO")) {
            throw new BusinessException("Loan cannot be approved");
        }

        loan.setStatus("APROVADO");
        loan.setApprovalDate(LocalDateTime.now());

        return repository.save(loan);
    }

    public Loan withdraw(Long id) {
        Loan loan = findOrFail(id);

        if (!loan.getStatus().equals("APROVADO")) {
            throw new BusinessException("Loan not approved");
        }

        Book book = loan.getBook();

        if (book.getAvailableQuantity() <= 0) {
            throw new BusinessException("Book unavailable");
        }

        loan.setStatus("RETIRADO");
        loan.setWithdrawableDate(LocalDateTime.now());
        loan.setDueDate(LocalDateTime.now().plusDays(7));

        book.setAvailableQuantity((short) (book.getAvailableQuantity() - 1));

        return repository.save(loan);
    }

    public Loan returnBook(Long id) {
        Loan loan = findOrFail(id);

        if (!loan.getStatus().equals("RETIRADO") && !loan.getStatus().equals("ATRASADO")) {
            throw new BusinessException("Loan cannot be returned");
        }

        loan.setReturnDate(LocalDateTime.now());
        loan.setStatus("DEVOLVIDO");

        Book book = loan.getBook();
        book.setAvailableQuantity((short) (book.getAvailableQuantity() + 1));

        return repository.save(loan);
    }

    private Loan findOrFail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found with id " + id));
    }
}