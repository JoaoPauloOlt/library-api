package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(LoanServiceIntegrationTest.FixedClockConfiguration.class)
@Transactional
class LoanServiceIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private Clock clock;

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
    void shouldReleaseReservedCopyWhenRequestedLoanIsCancelled() {
        Loan created = loanService.create(new CreateLoanCommand(bookId), userId);

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public boolean hasPermission(String permission) {
                return false;
            }
        };

        Loan cancelled = loanService.cancel(created.getId(), principal);

        assertThat(cancelled.getStatus()).isEqualTo(LoanStatus.CANCELED);
        assertThat(bookCopyRepository.findById(bookCopyId).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void shouldMarkOnlyOverdueActiveLoansAsLate() {
        LocalDateTime now = LocalDateTime.now(clock);

        BookCopy overdueCopy = bookCopyRepository.findById(bookCopyId).orElseThrow();
        overdueCopy.setStatus(CopyStatus.LOANED);
        bookCopyRepository.save(overdueCopy);

        Loan overdueLoan = new Loan();
        overdueLoan.setUser(userRepository.findById(userId).orElseThrow());
        overdueLoan.setBookCopy(overdueCopy);
        overdueLoan.setStatus(LoanStatus.ACTIVE);
        overdueLoan.setRequestDate(now.minusDays(8));
        overdueLoan.setApprovalDate(now.minusDays(7));
        overdueLoan.setWithdrawableDate(now.minusDays(7));
        overdueLoan.setDueDate(now.minusSeconds(1));
        overdueLoan = loanRepository.save(overdueLoan);

        User anotherUser = new User();
        anotherUser.setName("Future Loan User");
        anotherUser.setEmail("future-loan@example.com");
        anotherUser.setPassword("test-password");
        anotherUser.setTelephone("11999999998");
        anotherUser.setStatus(UserStatus.ACTIVE);
        anotherUser.setTokenVersion(0);
        anotherUser = userRepository.save(anotherUser);

        BookCopy futureCopy = new BookCopy();
        futureCopy.setBook(bookRepository.findById(bookId).orElseThrow());
        futureCopy.setBarcode("IT-LOAN-FUTURE-001");
        futureCopy.setStatus(CopyStatus.LOANED);
        futureCopy.setActive(true);
        futureCopy = bookCopyRepository.save(futureCopy);

        Loan futureLoan = new Loan();
        futureLoan.setUser(anotherUser);
        futureLoan.setBookCopy(futureCopy);
        futureLoan.setStatus(LoanStatus.ACTIVE);
        futureLoan.setRequestDate(now.minusDays(1));
        futureLoan.setApprovalDate(now);
        futureLoan.setWithdrawableDate(now);
        futureLoan.setDueDate(now.plusDays(1));
        futureLoan = loanRepository.save(futureLoan);

        int updatedLoans = loanService.markOverdueLoans();

        assertThat(updatedLoans).isEqualTo(1);
        assertThat(loanRepository.findById(overdueLoan.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanStatus.LATE);
        assertThat(loanRepository.findById(futureLoan.getId()).orElseThrow().getStatus())
                .isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    void shouldReturnLateLoanAndReleaseItsCopy() {
        LocalDateTime now = LocalDateTime.now(clock);
        BookCopy copy = bookCopyRepository.findById(bookCopyId).orElseThrow();
        copy.setStatus(CopyStatus.LOANED);
        bookCopyRepository.save(copy);

        Loan lateLoan = new Loan();
        lateLoan.setUser(userRepository.findById(userId).orElseThrow());
        lateLoan.setBookCopy(copy);
        lateLoan.setStatus(LoanStatus.LATE);
        lateLoan.setRequestDate(now.minusDays(8));
        lateLoan.setApprovalDate(now.minusDays(7));
        lateLoan.setWithdrawableDate(now.minusDays(7));
        lateLoan.setDueDate(now.minusDays(1));
        lateLoan = loanRepository.save(lateLoan);

        Loan returned = loanService.returnBook(lateLoan.getId());

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(returned.getReturnDate()).isEqualTo(now);
        assertThat(bookCopyRepository.findById(bookCopyId).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void shouldRejectNewLoanWhenUserAlreadyHasActiveLoanForBook() {
        BookCopy copy = bookCopyRepository.findById(bookCopyId).orElseThrow();
        copy.setStatus(CopyStatus.LOANED);
        bookCopyRepository.save(copy);

        LocalDateTime now = LocalDateTime.now(clock);
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

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
