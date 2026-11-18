package com.jpoltramari.library_api.application.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FineCalculationServiceTest {

    private final FineCalculationService service = new FineCalculationService();

    @Test
    void shouldNotChargeWhenReturnedOnDueDate() {
        assertThat(service.calculateLateDays(
                LocalDate.of(2026, 11, 18),
                LocalDate.of(2026, 11, 18))).isZero();
        assertThat(service.calculateAmount(0)).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateCalendarDaysAcrossMonthBoundary() {
        assertThat(service.calculateLateDays(
                LocalDate.of(2026, 11, 28),
                LocalDate.of(2026, 12, 1))).isEqualTo(3);
        assertThat(service.calculateAmount(3)).isEqualByComparingTo("6.00");
    }

    @Test
    void shouldRejectNegativeLateDays() {
        assertThatThrownBy(() -> service.calculateAmount(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingDates() {
        assertThatThrownBy(() -> service.calculateLateDays(null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
