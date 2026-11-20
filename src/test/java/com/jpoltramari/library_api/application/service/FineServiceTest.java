package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.domain.enums.FineStatus;
import com.jpoltramari.library_api.domain.model.Fine;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.FineRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    @Mock
    private FineRepository fineRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private UserRepository userRepository;

    private FineService service;

    @BeforeEach
    void setUp() {
        service = new FineService(
                fineRepository,
                new FineCalculationService(),
                outboxEventService,
                userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateFineAndRecordOutboxEventForLateReturn() {
        Loan loan = loanWithDueDate(LocalDateTime.parse("2026-11-10T10:00:00"));
        LocalDateTime returnedAt = LocalDateTime.parse("2026-11-13T10:00:00");
        when(fineRepository.existsByLoanId(7L)).thenReturn(false);
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> {
            Fine fine = invocation.getArgument(0);
            fine.setId(15L);
            return fine;
        });

        Fine result = service.createForReturnedLoan(loan, returnedAt);

        assertThat(result.getId()).isEqualTo(15L);
        assertThat(result.getLoan()).isSameAs(loan);
        assertThat(result.getUser()).isSameAs(loan.getUser());
        assertThat(result.getLateDays()).isEqualTo(3);
        assertThat(result.getDailyRate()).isEqualByComparingTo("2.00");
        assertThat(result.getAmount()).isEqualByComparingTo("6.00");
        assertThat(result.getStatus()).isEqualTo(FineStatus.OPEN);
        assertThat(result.getCreatedAt()).isEqualTo(NOW);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(outboxEventService).recordEvent(
                eq("FINE"), eq("15"), eq("fine.created"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("fineId", 15L)
                .containsEntry("loanId", 7L)
                .containsEntry("userId", 42L)
                .containsEntry("lateDays", 3)
                .containsEntry("currency", "BRL");
    }

    @Test
    void shouldNotCreateFineWhenReturnedOnDueDate() {
        Loan loan = loanWithDueDate(LocalDateTime.parse("2026-11-13T10:00:00"));

        Fine result = service.createForReturnedLoan(
                loan, LocalDateTime.parse("2026-11-13T10:00:00"));

        assertThat(result).isNull();
        verify(fineRepository, never()).existsByLoanId(anyLong());
        verify(fineRepository, never()).save(any(Fine.class));
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void shouldNotCreateFineWhenReturnedBeforeDueDate() {
        Loan loan = loanWithDueDate(LocalDateTime.parse("2026-11-14T10:00:00"));

        Fine result = service.createForReturnedLoan(
                loan, LocalDateTime.parse("2026-11-13T10:00:00"));

        assertThat(result).isNull();
        verify(fineRepository, never()).save(any(Fine.class));
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void shouldRejectDuplicateFineForLoan() {
        Loan loan = loanWithDueDate(LocalDateTime.parse("2026-11-10T10:00:00"));
        when(fineRepository.existsByLoanId(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.createForReturnedLoan(
                loan, LocalDateTime.parse("2026-11-13T10:00:00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A fine already exists for this loan.");

        verify(fineRepository, never()).save(any(Fine.class));
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void shouldRejectMissingDueDate() {
        Loan loan = loanWithDueDate(null);

        assertThatThrownBy(() -> service.createForReturnedLoan(
                loan, LocalDateTime.parse("2026-11-13T10:00:00")))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(fineRepository, outboxEventService);
    }

    @Test
    void shouldRejectMissingReturnDate() {
        Loan loan = loanWithDueDate(LocalDateTime.parse("2026-11-10T10:00:00"));

        assertThatThrownBy(() -> service.createForReturnedLoan(loan, null))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(fineRepository, outboxEventService);
    }

    private Loan loanWithDueDate(LocalDateTime dueDate) {
        Loan loan = new Loan();
        loan.setId(7L);
        loan.setDueDate(dueDate);
        User user = new User();
        user.setId(42L);
        loan.setUser(user);
        return loan;
    }
}
