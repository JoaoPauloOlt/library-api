package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.api.dto.dashboard.DashboardSummaryModel;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<LoanStatus> OVERDUE_STATUSES =
            List.of(LoanStatus.ACTIVE, LoanStatus.LATE);

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryModel getMySummary(Long userId, LocalDate from, LocalDate to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new IllegalArgumentException("The 'to' date must be after the 'from' date.");
        }

        LocalDateTime periodStart = from.atStartOfDay();
        LocalDateTime periodEnd = to.atStartOfDay();

        long totalBooks = bookRepository.count();
        long monthlyLoans = loanRepository
                .countByUserIdAndRequestDateGreaterThanEqualAndRequestDateLessThan(
                        userId, periodStart, periodEnd);
        long overdueLoans = loanRepository
                .countByUserIdAndStatusInAndDueDateBefore(
                        userId, OVERDUE_STATUSES, LocalDateTime.now());

        return new DashboardSummaryModel(totalBooks, monthlyLoans, overdueLoans);
    }
}
