package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.domain.enums.OutboxStatus;
import com.jpoltramari.library_api.domain.model.OutboxEvent;
import com.jpoltramari.library_api.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventDeliveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Mock
    private OutboxEventRepository repository;

    private OutboxEventDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new OutboxEventDeliveryService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldClaimEventsAndMoveTheirAvailabilityToLeaseExpiration() {
        OutboxEvent event = pendingEvent();
        when(repository.findAvailableForUpdate(eq(OutboxStatus.PENDING), eq(NOW), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<OutboxEventClaim> claims = service.claimAvailableEvents();

        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).id()).isEqualTo(event.getId());
        assertThat(claims.getFirst().eventType()).isEqualTo("loan.returned");
        assertThat(claims.getFirst().payload()).isEqualTo(event.getPayload());
        assertThat(claims.getFirst().leaseUntil()).isEqualTo(NOW.plusSeconds(180));
        assertThat(event.getAvailableAt()).isEqualTo(NOW.plusSeconds(180));
        verify(repository).saveAll(List.of(event));
    }

    @Test
    void shouldMarkClaimAsPublishedUsingLeaseFence() {
        OutboxEventClaim claim = claim();
        when(repository.markPublished(claim.id(), claim.leaseUntil(), NOW, OutboxStatus.PENDING, OutboxStatus.PUBLISHED)).thenReturn(1);

        assertThat(service.markPublished(claim)).isTrue();
    }

    @Test
    void shouldNotMarkPublishedWhenLeaseWasReclaimed() {
        OutboxEventClaim claim = claim();
        when(repository.markPublished(claim.id(), claim.leaseUntil(), NOW)).thenReturn(0);

        assertThat(service.markPublished(claim)).isFalse();
    }

    @Test
    void shouldScheduleExponentialRetryAfterFailure() {
        OutboxEventClaim claim = claim();
        when(repository.findAttemptsById(claim.id())).thenReturn(Optional.of(0));
        when(repository.markFailed(
                eq(claim.id()),
                eq(claim.leaseUntil()),
                eq(OutboxStatus.PENDING),
                eq(OutboxStatus.FAILED),
                eq(NOW.plusSeconds(5)),
                eq("broker unavailable")
        )).thenReturn(1);

        assertThat(service.markFailed(claim, new IllegalStateException("broker unavailable"))).isTrue();
    }

    @Test
    void shouldUseMaximumAttemptDelayAndFailAtAttemptLimit() {
        OutboxEventClaim claim = claim();
        when(repository.findAttemptsById(claim.id())).thenReturn(Optional.of(9));
        when(repository.markFailed(
                eq(claim.id()),
                eq(claim.leaseUntil()),
                eq(OutboxStatus.PENDING),
                eq(OutboxStatus.FAILED),
                eq(NOW),
                eq("connection refused")
        )).thenReturn(1);

        assertThat(service.markFailed(claim, new IllegalStateException("connection refused"))).isTrue();
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
        event.setCreatedAt(NOW);
        event.setAvailableAt(NOW);
        return event;
    }

    private OutboxEventClaim claim() {
        return new OutboxEventClaim(
                UUID.randomUUID(),
                "loan.returned",
                "{\"eventId\":\"test\"}",
                NOW.plusSeconds(180)
        );
    }
}
