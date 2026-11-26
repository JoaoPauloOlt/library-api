package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.readmodel.DashboardGenreMetric;
import com.jpoltramari.library_api.application.readmodel.DashboardSummary;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
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
        service = new DashboardService(bookRepository, loanRepository, Clock.systemUTC());
    }

    @Test
    void shouldReturnAggregatedMetricsForAuthenticatedUserAndRequestedPeriod() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 12, 1);
        when(bookRepository.count()).thenReturn(120L);
        LocalDateTime periodStart = LocalDateTime.of(2026, 11, 1, 3, 0);
        LocalDateTime periodEnd = LocalDateTime.of(2026, 12, 1, 3, 0);
        when(loanRepository.countByUserIdAndRequestDateGreaterThanEqualAndRequestDateLessThan(
                42L, periodStart, periodEnd)).thenReturn(14L);
        when(loanRepository.countByUserIdAndStatusInAndDueDateBefore(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq(List.of(LoanStatus.ACTIVE, LoanStatus.LATE)),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(3L);

        DashboardSummary result = service.getMySummary(42L, from, to);

        assertEquals(new DashboardSummary(120L, 14L, 3L), result);
        verify(bookRepository).count();
        verify(loanRepository).countByUserIdAndRequestDateGreaterThanEqualAndRequestDateLessThan(
                42L, periodStart, periodEnd);
        verify(loanRepository).countByUserIdAndStatusInAndDueDateBefore(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq(List.of(LoanStatus.ACTIVE, LoanStatus.LATE)),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void shouldReturnGenreMetricsForTheWholeCatalog() {
        List<DashboardGenreMetric> metrics =
                List.of(new DashboardGenreMetric(Genre.FANTASY, 12L));
        when(bookRepository.findDashboardGenreMetrics(
                List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE)))
                .thenReturn(metrics);

        assertEquals(metrics, service.getGenreMetrics());
        verify(bookRepository).findDashboardGenreMetrics(
                List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE));
    }

    @Test
    void shouldReturnBooksForRequestedGenreUsingTheRequestedPage() {
        Pageable pageable = Pageable.ofSize(4);
        Page<Book> expected = new PageImpl<>(List.of(new Book()));
        when(bookRepository.findDashboardGenreBooks(
                Genre.FANTASY,
                List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE),
                pageable)).thenReturn(expected);

        assertEquals(expected, service.getGenreBooks(Genre.FANTASY, pageable));
        verify(bookRepository).findDashboardGenreBooks(
                Genre.FANTASY,
                List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE),
                pageable);
    }

    @Test
    void shouldReturnAvailableHighlightsUsingTheRequestedPage() {
        Pageable pageable = Pageable.ofSize(5);
        Page<Book> expected = new PageImpl<>(List.of(new Book()));
        when(bookRepository.findDashboardAvailableHighlights(
                CopyStatus.AVAILABLE,
                List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE),
                pageable)).thenReturn(expected);

        assertEquals(expected, service.getAvailableHighlights(pageable));
        verify(bookRepository).findDashboardAvailableHighlights(
                CopyStatus.AVAILABLE,
                List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE),
                pageable);
    }

    @Test
    void shouldReturnMostBorrowedBooksUsingGroupedQuery() {
        Pageable pageable = Pageable.ofSize(5);
        Page<Book> expected = new PageImpl<>(List.of(new Book()));
        when(bookRepository.findDashboardMostBorrowed(
                List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE),
                pageable)).thenReturn(expected);

        assertEquals(expected, service.getMostBorrowed(pageable));
        verify(bookRepository).findDashboardMostBorrowed(
                List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE),
                pageable);
    }

    @Test
    void shouldRejectNullStartDate() {
        assertThrows(BusinessException.class,
                () -> service.getMySummary(42L, null, LocalDate.of(2026, 12, 1)));
        verify(bookRepository, never()).count();
    }

    @Test
    void shouldRejectNullEndDate() {
        assertThrows(BusinessException.class,
                () -> service.getMySummary(42L, LocalDate.of(2026, 11, 1), null));
        verify(bookRepository, never()).count();
    }

    @Test
    void shouldRejectEndDateThatIsNotAfterStartDate() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        assertThrows(BusinessException.class,
                () -> service.getMySummary(42L, date, date));
        assertThrows(BusinessException.class,
                () -> service.getMySummary(42L, date, date.minusDays(1)));
        verify(bookRepository, never()).count();
    }
}