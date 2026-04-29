package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.LoanInput;
import com.jpoltramari.library_api.domain.exception.BookNotFound;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
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

    @Autowired
    private UserRepository userRepository;

    public Page<Loan> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    public Loan create(LoanInput input){

        Book book = bookRepository.findById(input.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id " + input.getBookId()));

        if (!book.getAvailable()){
            throw new BusinessException("Book is already loaned");
        }

        User user = userRepository.findById(input.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setUser(user);
        loan.setLoanDate(LocalDateTime.now());

        book.setAvailable(false);

        return repository.save(loan);
    }

    public Loan returnBook(Long id){

        Loan loan = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));

        if (loan.getReturnDate() != null){
            throw new BusinessException("This loan has already been returned");
        }

        loan.setReturnDate(LocalDateTime.now());
        loan.getBook().setAvailable(true);

        return repository.save(loan);
    }
}