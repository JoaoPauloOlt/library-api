package com.jpoltramari.library_api.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OpenLoanPerCopyConstraintIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long secondUserId;
    private Long bookCopyId;

    @BeforeEach
    void setUp() {
        userId = createUser("first");
        secondUserId = createUser("second");
        long bookId = jdbcTemplate.queryForObject("""
                INSERT INTO books (isbn, title, genre)
                VALUES (?, 'Open Loan Constraint Book', 'CLASSIC')
                RETURNING id
                """, Long.class, uniqueIsbn());
        bookCopyId = jdbcTemplate.queryForObject("""
                INSERT INTO book_copies (book_id, barcode, status, active, updated_at)
                VALUES (?, ?, 'AVAILABLE', TRUE, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, bookId, "OPEN-LOAN-" + UUID.randomUUID());
    }

    @Test
    void shouldRejectSecondOpenLoanForTheSamePhysicalCopy() {
        insertLoan(userId, "REQUESTED");

        assertThatThrownBy(() -> insertLoan(secondUserId, "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowMultipleHistoricalLoansForTheSamePhysicalCopy() {
        assertThatCode(() -> {
            insertLoan(userId, "RETURNED");
            insertLoan(secondUserId, "CANCELED");
        }).doesNotThrowAnyException();
    }

    private Long createUser(String suffix) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (name, email, password, telephone, status, token_version)
                VALUES (?, ?, 'test-password', '11999999999', 'ACTIVE', 0)
                RETURNING id
                """, Long.class,
                "Open Loan Constraint " + suffix,
                "open-loan-" + suffix + "-" + UUID.randomUUID() + "@example.com");
    }

    private void insertLoan(Long loanUserId, String status) {
        jdbcTemplate.update("""
                INSERT INTO loans (user_id, book_copy_id, status, request_date)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """, loanUserId, bookCopyId, status);
    }

    private String uniqueIsbn() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 13);
    }
}
