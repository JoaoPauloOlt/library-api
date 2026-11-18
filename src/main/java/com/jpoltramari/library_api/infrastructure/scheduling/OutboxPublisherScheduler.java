package com.jpoltramari.library_api.infrastructure.scheduling;

import com.jpoltramari.library_api.domain.enums.OutboxStatus;
import com.jpoltramari.library_api.domain.model.OutboxEvent;
import com.jpoltramari.library_api.domain.repository.OutboxEventRepository;
import com.jpoltramari.library_api.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Profile({"dev", "prod"})
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.messaging.outbox-poll-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        Instant now = clock.instant();
        List<OutboxEvent> events = repository.findAvailableForUpdate(
                OutboxStatus.PENDING, now, PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : events) {
            try {
                publish(event);
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(clock.instant());
                event.setLastError(null);
            } catch (Exception exception) {
                registerFailure(event, exception);
            }
        }
    }

    private void publish(OutboxEvent event) throws Exception {
        CorrelationData correlationData = new CorrelationData(event.getId().toString());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENTS_EXCHANGE,
                event.getEventType(),
                event.getPayload(),
                correlationData
        );

        CorrelationData.Confirm confirm = correlationData.getFuture().get(10, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ rejected event: " + confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ returned an unroutable event.");
        }
    }

    private void registerFailure(OutboxEvent event, Exception exception) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(safeMessage(exception));

        if (attempts >= MAX_ATTEMPTS) {
            event.setStatus(OutboxStatus.FAILED);
            return;
        }

        long backoffSeconds = Math.min(3600L, 5L * (1L << Math.min(attempts - 1, 9)));
        event.setAvailableAt(clock.instant().plusSeconds(backoffSeconds));
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
