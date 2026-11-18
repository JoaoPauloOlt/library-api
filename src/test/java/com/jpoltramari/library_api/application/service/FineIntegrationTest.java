package com.jpoltramari.library_api.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.FineStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Fine;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.OutboxEvent;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.FineRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.OutboxEventRepository;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(FineIntegrationTest.FixedClockConfiguration.class)
@Transactional
class FineIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");

    @Autowired private LoanService loanService;
    @Autowired private FineService fineService;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private FineRepository fineRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private ObjectMapper objectMapper;

    private User borrower;
    private User administrator;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        borrower = saveUser("fine-borrower");
        administrator = saveUser("fine-admin");

        Book book = new Book();
        book.setIsbn(randomIsbn());
        book.setTitle("Fine Integration Book");
        book.setGenre(Genre.CLASSIC);
        book = bookRepository.save(book);

        copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode("FINE-IT-" + UUID.randomUUID());
        copy.setStatus(CopyStatus.LOANED);
        copy.setActive(true);
        copy = bookCopyRepository.save(copy);
    }

    @Test
    void shouldReturnLateLoanAndPersistFineAndBothOutboxEventsAtomically() throws Exception {
        Loan loan = saveLoan(LocalDateTime.parse("2026-11-15T12:00:00"));

        Loan returned = loanService.returnBook(loan.getId());

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(returned.getReturnDate()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(bookCopyRepository.findById(copy.getId()).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.AVAILABLE);

        Fine fine = fineRepository.findByLoanId(loan.getId()).orElseThrow();
        assertThat(fine.getUser().getId()).isEqualTo(borrower.getId());
        assertThat(fine.getLateDays()).isEqualTo(3);
        assertThat(fine.getDailyRate()).isEqualByComparingTo("2.00");
        assertThat(fine.getAmount()).isEqualByComparingTo("6.00");
        assertThat(fine.getStatus()).isEqualTo(FineStatus.OPEN);

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("fine.created", "loan.returned");

        OutboxEvent fineCreated = events.stream()
                .filter(event -> "fine.created".equals(event.getEventType()))
                .findFirst().orElseThrow();
        JsonNode payload = objectMapper.readTree(fineCreated.getPayload());
        assertThat(payload.path("data").path("fineId").asLong()).isEqualTo(fine.getId());
        assertThat(payload.path("data").path("lateDays").asInt()).isEqualTo(3);
        assertThat(payload.path("data").path("amount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    void shouldNotCreateFineForReturnOnDueCalendarDate() {
        Loan loan = saveLoan(LocalDateTime.parse("2026-11-18T12:00:00"));

        Loan returned = loanService.returnBook(loan.getId());

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(fineRepository.findByLoanId(loan.getId())).isEmpty();
        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getEventType)
                .containsExactly("loan.returned");
    }

    @Test
    void shouldPersistOnlyOneFineForTheSameLoan() {
        Loan loan = saveLoan(LocalDateTime.parse("2026-11-15T12:00:00"));
        LocalDateTime returnedAt = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

        Fine created = fineService.createForReturnedLoan(loan, returnedAt);

        assertThat(created).isNotNull();
        assertThatThrownBy(() -> fineService.createForReturnedLoan(loan, returnedAt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A fine already exists for this loan.");
        assertThat(fineRepository.findByLoanId(loan.getId())).isPresent();
        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getEventType)
                .containsExactly("fine.created");
    }

    @Test
    void shouldWaiveOpenFineAndPersistAuditAndOutboxEvent() throws Exception {
        Loan loan = saveLoan(LocalDateTime.parse("2026-11-15T12:00:00"));
        Fine fine = fineService.createForReturnedLoan(
                loan, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));

        Fine waived = fineService.waive(fine.getId(), "  Administrative review  ", administrator.getId());

        assertThat(waived.getStatus()).isEqualTo(FineStatus.WAIVED);
        assertThat(waived.getWaivedAt()).isEqualTo(NOW);
        assertThat(waived.getWaivedBy().getId()).isEqualTo(administrator.getId());
        assertThat(waived.getWaiverReason()).isEqualTo("Administrative review");

        Fine persisted = fineRepository.findById(fine.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(FineStatus.WAIVED);
        assertThat(persisted.getWaivedBy().getId()).isEqualTo(administrator.getId());

        OutboxEvent waivedEvent = outboxEventRepository.findAll().stream()
                .filter(event -> "fine.waived".equals(event.getEventType()))
                .findFirst().orElseThrow();
        JsonNode payload = objectMapper.readTree(waivedEvent.getPayload());
        assertThat(payload.path("data").path("fineId").asLong()).isEqualTo(fine.getId());
        assertThat(payload.path("data").path("waivedBy").asLong()).isEqualTo(administrator.getId());
        assertThat(payload.path("data").path("reason").asText()).isEqualTo("Administrative review");
    }

    @Test
    void shouldRejectWaivingFineMoreThanOnce() {
        Loan loan = saveLoan(LocalDateTime.parse("2026-11-15T12:00:00"));
        Fine fine = fineService.createForReturnedLoan(
                loan, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        fineService.waive(fine.getId(), "First waiver", administrator.getId());

        assertThatThrownBy(() -> fineService.waive(
                fine.getId(), "Second waiver", administrator.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only open fines can be waived.");

        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getEventType)
                .containsExactly("fine.created", "fine.waived");
    }

    private User saveUser(String prefix) {
        User user = new User();
        user.setName(prefix);
        user.setEmail(prefix + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword("integration-test-password");
        user.setTelephone("11999999999");
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(0);
        return userRepository.save(user);
    }

    private Loan saveLoan(LocalDateTime dueDate) {
        Loan loan = new Loan();
        loan.setUser(borrower);
        loan.setBookCopy(copy);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setRequestDate(dueDate.minusDays(8));
        loan.setApprovalDate(dueDate.minusDays(7));
        loan.setWithdrawableDate(dueDate.minusDays(7));
        loan.setDueDate(dueDate);
        return loanRepository.save(loan);
    }

    private String randomIsbn() {
        return "978" + String.format("%010d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
