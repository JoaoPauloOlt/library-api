package com.jpoltramari.library_api.infrastructure.scheduling;

import com.jpoltramari.library_api.application.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanOverdueSchedulerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanOverdueScheduler scheduler;

    @Test
    void shouldDelegateOverdueProcessingToLoanService() {
        scheduler.markOverdueLoans();

        verify(loanService).markOverdueLoans();
    }
}
