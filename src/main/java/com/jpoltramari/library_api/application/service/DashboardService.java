package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.readmodel.DashboardGenreMetric;
import com.jpoltramari.library_api.application.readmodel.DashboardSummary;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<LoanStatus> OVERDUE_STATUSES =
            List.of(LoanStatus.ACTIVE, LoanStatus.LATE);
    private static final List<LoanStatus> COUNTED_LOAN_STATUSES =
            List.of(LoanStatus.ACTIVE, LoanStatus.RETURNED, LoanStatus.LATE);

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardSummary getMySummary(Long userId, LocalDate from, LocalDate to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new BusinessException("The 'to' date must be after the 'from' date.");
        }

        LocalDateTime periodStart = from.atStartOfDay();
        LocalDateTime periodEnd = to.atStartOfDay();

        long totalBooks = bookRepository.count();
        long monthlyLoans = loanRepository
                .countByUserIdAndRequestDateGreaterThanEqualAndRequestDateLessThan(
                        userId, periodStart, periodEnd);
        long overdueLoans = loanRepository
                .countByUserIdAndStatusInAndDueDateBefore(
                        userId, OVERDUE_STATUSES, LocalDateTime.now(clock));

        return new DashboardSummary(totalBooks, monthlyLoans, overdueLoans);
    }

    @Transactional(readOnly = true)
    public List<DashboardGenreMetric> getGenreMetrics() {
        return bookRepository.findDashboardGenreMetrics(COUNTED_LOAN_STATUSES);
    }

    @Transactional(readOnly = true)
    public Page<Book> getGenreBooks(Genre genre, Pageable pageable) {
        return bookRepository.findDashboardGenreBooks(
                genre, COUNTED_LOAN_STATUSES, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Book> getMostBorrowed(Pageable pageable) {
        return bookRepository.findDashboardMostBorrowed(COUNTED_LOAN_STATUSES, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Book> getAvailableHighlights(Pageable pageable) {
        return bookRepository.findDashboardAvailableHighlights(
                CopyStatus.AVAILABLE, COUNTED_LOAN_STATUSES, pageable);
    }
}