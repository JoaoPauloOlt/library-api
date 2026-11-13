package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanServiceIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    private Long userId;
    private Long bookId;
    private Long bookCopyId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Loan Integration User");
        user.setEmail("loan-integration@example.com");
        user.setPassword("test-password");
        user.setTelephone("11999999999");
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(0);
        userId = userRepository.save(user).getId();

        Book book = new Book();
        book.setIsbn("9780000000001");
        book.setTitle("Loan Integration Book");
        book.setGenre(Genre.CLASSIC);
        bookId = bookRepository.save(book).getId();

        BookCopy copy = new BookCopy();
        copy.setBook(bookRepository.findById(bookId).orElseThrow());
        copy.setBarcode("IT-LOAN-001");
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        bookCopyId = bookCopyRepository.save(copy).getId();
    }

    @Test
    void shouldCreateApproveAndReturnLoanThroughDatabase() {
        Loan created = loanService.create(
                new CreateLoanCommand(bookId),
                userId
        );

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(LoanStatus.REQUESTED);
        assertThat(created.getRequestDate()).isNotNull();
        assertThat(bookCopyRepository.findById(bookCopyId).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.RESERVED);

        Loan approved = loanService.approve(created.getId());

        assertThat(approved.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(approved.getApprovalDate()).isNotNull();
        assertThat(approved.getWithdrawableDate()).isNotNull();
        assertThat(approved.getDueDate()).isNotNull();
        assertThat(approved.getDueDate()).isAfter(approved.getApprovalDate());
        assertThat(approved.getBookCopy().getStatus()).isEqualTo(CopyStatus.LOANED);

        Loan returned = loanService.returnBook(created.getId());

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(returned.getReturnDate()).isNotNull();
        assertThat(returned.getBookCopy().getStatus()).isEqualTo(CopyStatus.AVAILABLE);

        Loan persisted = loanRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(persisted.getReturnDate()).isNotNull();
    }

    @Test
    void shouldRejectNewLoanWhenUserAlreadyHasActiveLoanForBook() {
        BookCopy copy = bookCopyRepository.findById(bookCopyId).orElseThrow();
        copy.setStatus(CopyStatus.LOANED);
        bookCopyRepository.save(copy);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Loan activeLoan = new Loan();
        activeLoan.setUser(userRepository.findById(userId).orElseThrow());
        activeLoan.setBookCopy(copy);
        activeLoan.setStatus(LoanStatus.ACTIVE);
        activeLoan.setRequestDate(now.minusDays(2));
        activeLoan.setApprovalDate(now.minusDays(1));
        activeLoan.setWithdrawableDate(now.minusDays(1));
        activeLoan.setDueDate(now.plusDays(6));
        loanRepository.save(activeLoan);

        assertThatThrownBy(() -> loanService.create(
                new CreateLoanCommand(bookId),
                userId
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an active loan");
    }
}
