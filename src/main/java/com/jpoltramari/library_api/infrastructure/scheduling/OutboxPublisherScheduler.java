package com.jpoltramari.library_api.infrastructure.scheduling;

import com.jpoltramari.library_api.application.service.OutboxEventClaim;
import com.jpoltramari.library_api.application.service.OutboxEventDeliveryService;
import com.jpoltramari.library_api.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Profile({"dev", "prod"})
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherScheduler {

    private final OutboxEventDeliveryService deliveryService;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${app.messaging.outbox-poll-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEventClaim> claims = deliveryService.claimAvailableEvents();

        for (OutboxEventClaim claim : claims) {
            try {
                publish(claim);
                if (!deliveryService.markPublished(claim)) {
                    log.warn("Outbox event {} was published but its claim lease was lost before acknowledgement.", claim.id());
                }
            } catch (Exception exception) {
                boolean updated = deliveryService.markFailed(claim, exception);
                if (!updated) {
                    log.warn("Could not register failure for outbox event {} because its claim lease is no longer current.", claim.id());
                }
            }
        }
    }

    private void publish(OutboxEventClaim claim) throws Exception {
        CorrelationData correlationData = new CorrelationData(claim.id().toString());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENTS_EXCHANGE,
                claim.eventType(),
                claim.payload(),
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
}
