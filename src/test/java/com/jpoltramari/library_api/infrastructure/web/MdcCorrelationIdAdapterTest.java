package com.jpoltramari.library_api.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class MdcCorrelationIdAdapterTest {

    @Test
    void shouldReturnCorrelationIdFromMdc() {
        MDC.put("correlationId", "test-correlation-id");

        try {
            assertThat(new MdcCorrelationIdAdapter().getCorrelationId())
                    .contains("test-correlation-id");
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Test
    void shouldReturnEmptyWhenCorrelationIdIsAbsent() {
        MDC.remove("correlationId");

        assertThat(new MdcCorrelationIdAdapter().getCorrelationId())
                .isEmpty();
    }
}
