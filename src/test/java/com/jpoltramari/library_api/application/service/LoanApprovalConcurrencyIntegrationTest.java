package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoanApprovalConcurrencyIntegrationTest {

    @Autowired private LoanService loanService;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private LoanRepository loanRepository;

    private final List<Long> userIds = new ArrayList<>();
    private Long bookId;
    private Long copyId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Loan Approval User");
        user.setEmail("loan-approval-" + UUID.randomUUID() + "@example.com");
        user.setPassword("test-password");
        user.setTelephone("11999999999");
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(0);
        userIds.add(userRepository.save(user).getId());

        Book book = new Book();
        book.setIsbn(UUID.randomUUID().toString().replace("-", "").substring(0, 13));
        book.setTitle("Loan Approval Book");
        book.setGenre(Genre.CLASSIC);
        bookId = bookRepository.save(book).getId();

        BookCopy copy = new BookCopy();
        copy.setBook(bookRepository.findById(bookId).orElseThrow());
        copy.setBarcode("IT-APPROVAL-" + UUID.randomUUID());
        copy.setStatus(CopyStatus.RESERVED);
        copy.setActive(true);
        copyId = bookCopyRepository.save(copy).getId();
    }

    @AfterEach
    void cleanUp() {
        if (bookId != null) {
            loanRepository.deleteAll(loanRepository.findByBookCopyBookId(bookId));
            if (copyId != null) {
                bookCopyRepository.deleteById(copyId);
            }
            bookRepository.deleteById(bookId);
        }
        if (!userIds.isEmpty()) {
            userRepository.deleteAllById(userIds);
            userIds.clear();
        }
    }

    @Test
    void shouldApproveSingleOpenLoanAndMarkCopyAsLoaned() {
        BookCopy copy = bookCopyRepository.findById(copyId).orElseThrow();
        Loan loan = new Loan();
        loan.setUser(userRepository.findById(userIds.get(0)).orElseThrow());
        loan.setBookCopy(copy);
        loan.setStatus(LoanStatus.REQUESTED);
        loan.setRequestDate(LocalDateTime.now(ZoneOffset.UTC));
        Loan requestedLoan = loanRepository.save(loan);

        Loan approvedLoan = loanService.approve(requestedLoan.getId());

        assertThat(approvedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loanRepository.findByBookCopyBookId(bookId))
                .singleElement()
                .extracting(Loan::getStatus)
                .isEqualTo(LoanStatus.ACTIVE);
        assertThat(bookCopyRepository.findById(copyId).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.LOANED);
    }
}
