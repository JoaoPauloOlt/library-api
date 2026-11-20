package com.jpoltramari.library_api.infrastructure.scheduling;

import com.jpoltramari.library_api.application.service.OutboxEventClaim;
import com.jpoltramari.library_api.application.service.OutboxEventDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherSchedulerTest {

    @Mock
    private OutboxEventDeliveryService deliveryService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxPublisherScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxPublisherScheduler(deliveryService, rabbitTemplate);
    }

    @Test
    void shouldMarkEventAsPublishedAfterBrokerAck() {
        OutboxEventClaim claim = claim();
        when(deliveryService.claimAvailableEvents()).thenReturn(List.of(claim));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
        when(deliveryService.markPublished(claim)).thenReturn(true);

        scheduler.publishPendingEvents();

        verify(deliveryService).markPublished(claim);
        verify(deliveryService, never()).markFailed(eq(claim), any(Exception.class));
    }

    @Test
    void shouldRegisterFailureAfterNegativeBrokerAck() {
        OutboxEventClaim claim = claim();
        when(deliveryService.claimAvailableEvents()).thenReturn(List.of(claim));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker unavailable"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
        when(deliveryService.markFailed(eq(claim), any(Exception.class))).thenReturn(true);

        scheduler.publishPendingEvents();

        verify(deliveryService).markFailed(eq(claim), any(Exception.class));
        verify(deliveryService, never()).markPublished(claim);
    }

    @Test
    void shouldRegisterFailureWhenPublishingThrows() {
        OutboxEventClaim claim = claim();
        when(deliveryService.claimAvailableEvents()).thenReturn(List.of(claim));
        doThrow(new IllegalStateException("connection refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
        when(deliveryService.markFailed(eq(claim), any(Exception.class))).thenReturn(true);

        scheduler.publishPendingEvents();

        verify(deliveryService).markFailed(eq(claim), any(Exception.class));
    }

    private OutboxEventClaim claim() {
        return new OutboxEventClaim(
                UUID.randomUUID(),
                "loan.returned",
                "{\"eventId\":\"test\"}",
                Instant.parse("2026-11-18T15:03:00Z")
        );
    }
}
