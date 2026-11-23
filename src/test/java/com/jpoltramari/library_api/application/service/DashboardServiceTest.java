package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.readmodel.DashboardSummary;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private LoanRepository loanRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(bookRepository, loanRepository);
    }

    @Test
    void shouldReturnAggregatedMetricsForAuthenticatedUserAndRequestedPeriod() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 12, 1);
        when(bookRepository.count()).thenReturn(120L);
        when(loanRepository.countByUserIdAndRequestDateGreaterThanEqualAndRequestDateLessThan(
                42L, from.atStartOfDay(), to.atStartOfDay())).thenReturn(14L);
        when(loanRepository.countByUserIdAndStatusInAndDueDateBefore(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq(List.of(LoanStatus.ACTIVE, LoanStatus.LATE)),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(3L);

        DashboardSummary result = service.getMySummary(42L, from, to);

        assertEquals(new DashboardSummary(120L, 14L, 3L), result);
        verify(bookRepository).count();
        verify(loanRepository).countByUserIdAndRequestDateGreaterThanEqualAndRequestDateLessThan(
                42L, from.atStartOfDay(), to.atStartOfDay());
        verify(loanRepository).countByUserIdAndStatusInAndDueDateBefore(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq(List.of(LoanStatus.ACTIVE, LoanStatus.LATE)),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void shouldRejectNullStartDate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getMySummary(42L, null, LocalDate.of(2026, 12, 1)));
        verify(bookRepository, never()).count();
    }

    @Test
    void shouldRejectNullEndDate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getMySummary(42L, LocalDate.of(2026, 11, 1), null));
        verify(bookRepository, never()).count();
    }

    @Test
    void shouldRejectEndDateThatIsNotAfterStartDate() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        assertThrows(IllegalArgumentException.class,
                () -> service.getMySummary(42L, date, date));
        assertThrows(IllegalArgumentException.class,
                () -> service.getMySummary(42L, date, date.minusDays(1)));
        verify(bookRepository, never()).count();
    }
}
