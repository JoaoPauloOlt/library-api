package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.domain.enums.FineStatus;
import com.jpoltramari.library_api.domain.model.Fine;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FineService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final FineRepository fineRepository;
    private final FineCalculationService calculationService;
    private final OutboxEventService outboxEventService;
    private final Clock clock;

    /**
     * Creates the fine in the same transaction as the loan return.
     * LocalDateTime values in the circulation model are interpreted as UTC,
     * matching the application's Clock.systemUTC() configuration.
     */
    @Transactional
    public Fine createForReturnedLoan(Loan loan, LocalDateTime returnedAt) {
        if (loan.getDueDate() == null || returnedAt == null) {
            throw new IllegalStateException("Loan due date and return date are required to calculate a fine.");
        }

        LocalDate dueDate = loan.getDueDate().toInstant(ZoneOffset.UTC)
                .atZone(BUSINESS_ZONE).toLocalDate();
        LocalDate returnDate = returnedAt.toInstant(ZoneOffset.UTC)
                .atZone(BUSINESS_ZONE).toLocalDate();

        int lateDays = calculationService.calculateLateDays(dueDate, returnDate);
        if (lateDays == 0) {
            return null;
        }

        if (fineRepository.existsByLoanId(loan.getId())) {
            throw new IllegalStateException("A fine already exists for this loan.");
        }

        Fine fine = new Fine();
        fine.setLoan(loan);
        fine.setUser(loan.getUser());
        fine.setLateDays(lateDays);
        fine.setDailyRate(calculationService.getDailyRate());
        fine.setAmount(calculationService.calculateAmount(lateDays));
        fine.setStatus(FineStatus.OPEN);
        fine.setCreatedAt(clock.instant());

        Fine saved = fineRepository.save(fine);
        outboxEventService.record(
                "FINE",
                saved.getId().toString(),
                "fine.created",
                Map.of(
                        "fineId", saved.getId(),
                        "loanId", loan.getId(),
                        "userId", loan.getUser().getId(),
                        "lateDays", lateDays,
                        "dailyRate", saved.getDailyRate(),
                        "amount", saved.getAmount(),
                        "currency", "BRL"
                )
        );
        return saved;
    }
}
