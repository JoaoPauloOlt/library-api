package com.jpoltramari.library_api.infrastructure.scheduling;

import com.jpoltramari.library_api.application.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class LoanOverdueScheduler {

    private final LoanService loanService;

    @Scheduled(cron = "${app.loans.overdue-check-cron:0 */5 * * * *}", zone = "UTC")
    public void markOverdueLoans() {
        loanService.markOverdueLoans();
    }
}
