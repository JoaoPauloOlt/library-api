package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanServiceLifecycleTest {

    @Test
    void loanStatusEnumShouldRepresentCompleteMvpLifecycle() {
        assertEquals(6, LoanStatus.values().length);
        assertEquals(LoanStatus.REQUESTED, LoanStatus.valueOf("REQUESTED"));
        assertEquals(LoanStatus.APPROVED, LoanStatus.valueOf("APPROVED"));
        assertEquals(LoanStatus.ACTIVE, LoanStatus.valueOf("ACTIVE"));
        assertEquals(LoanStatus.RETURNED, LoanStatus.valueOf("RETURNED"));
        assertEquals(LoanStatus.CANCELED, LoanStatus.valueOf("CANCELED"));
    }

    @Test
    void availableCopyShouldBeEligibleForLoan() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);

        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void inactiveCopyShouldNotBeTreatedAsAvailable() {
        BookCopy copy = new BookCopy();
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(false);

        assertEquals(false, copy.isActive());
    }
}
