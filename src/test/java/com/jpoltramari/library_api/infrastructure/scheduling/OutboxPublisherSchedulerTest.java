package com.jpoltramari.library_api.infrastructure.scheduling;

import com.jpoltramari.library_api.domain.enums.OutboxStatus;
import com.jpoltramari.library_api.domain.model.OutboxEvent;
import com.jpoltramari.library_api.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherSchedulerTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneOffset.UTC);
    private OutboxPublisherScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxPublisherScheduler(repository, rabbitTemplate, clock);
    }

    @Test
    void shouldMarkEventAsPublishedAfterBrokerAck() {
        OutboxEvent event = pendingEvent();
        when(repository.findAvailableForUpdate(eq(OutboxStatus.PENDING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        scheduler.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(clock.instant());
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void shouldScheduleRetryAfterNegativeBrokerAck() {
        OutboxEvent event = pendingEvent();
        when(repository.findAvailableForUpdate(eq(OutboxStatus.PENDING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker unavailable"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        scheduler.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getAvailableAt()).isEqualTo(clock.instant().plusSeconds(5));
        assertThat(event.getLastError()).contains("broker unavailable");
    }

    @Test
    void shouldMoveEventToFailedAfterMaximumAttempts() {
        OutboxEvent event = pendingEvent();
        event.setAttempts(9);
        when(repository.findAvailableForUpdate(eq(OutboxStatus.PENDING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        doThrow(new IllegalStateException("connection refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        scheduler.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(10);
        assertThat(event.getLastError()).contains("connection refused");
    }

    private OutboxEvent pendingEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("LOAN");
        event.setAggregateId("42");
        event.setEventType("loan.returned");
        event.setPayload("{\"eventId\":\"test\"}");
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(clock.instant());
        event.setAvailableAt(clock.instant());
        return event;
    }
}
