package com.jpoltramari.library_api.application.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class FineCalculationService {

    private static final BigDecimal DAILY_RATE = new BigDecimal("2.00");

    public int calculateLateDays(LocalDate dueDate, LocalDate returnDate) {
        if (dueDate == null || returnDate == null) {
            throw new IllegalArgumentException("Due date and return date are required.");
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate));
    }

    public BigDecimal calculateAmount(int lateDays) {
        if (lateDays < 0) {
            throw new IllegalArgumentException("Late days cannot be negative.");
        }
        return DAILY_RATE.multiply(BigDecimal.valueOf(lateDays));
    }

    public BigDecimal getDailyRate() {
        return DAILY_RATE;
    }
}
