package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class LoanServiceIntegrationTest {

    private static final String USER_EMAIL = "user@library.com";
    private static final String AVAILABLE_BOOK_ISBN = "9780743273565";
    private static final String ACTIVE_BOOK_ISBN = "9780451524935";

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Test
    void shouldCreateApproveAndReturnLoanThroughDatabase() {
        User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
        Long bookId = bookCopyRepository.findByBarcode("BC-GATSBY-001")
                .orElseThrow()
                .getBook()
                .getId();

        Loan created = loanService.create(
                new com.jpoltramari.library_api.api.dto.loan.LoanInput(bookId),
                user.getId()
        );

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(LoanStatus.REQUESTED);
        assertThat(created.getRequestDate()).isNotNull();

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
        User user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
        Long bookId = bookCopyRepository.findByBarcode("BC-1984-001")
                .orElseThrow()
                .getBook()
                .getId();

        assertThatThrownBy(() -> loanService.create(
                new com.jpoltramari.library_api.api.dto.loan.LoanInput(bookId),
                user.getId()
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an active loan");
    }
}
