package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.domain.enums.OutboxStatus;
import com.jpoltramari.library_api.domain.model.OutboxEvent;
import com.jpoltramari.library_api.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventDeliveryService {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 10;
    private static final long LEASE_SECONDS = 60;

    private final OutboxEventRepository repository;
    private final Clock clock;

    /**
     * Claims a batch in a short transaction. The lease prevents another
     * scheduler instance from claiming the same rows while publication occurs.
     */
    @Transactional
    public List<OutboxEventClaim> claimAvailableEvents() {
        Instant now = clock.instant();
        Instant leaseUntil = now.plusSeconds(LEASE_SECONDS);

        List<OutboxEvent> events = repository.findAvailableForUpdate(
                OutboxStatus.PENDING, now, PageRequest.of(0, BATCH_SIZE));

        return events.stream()
                .map(event -> {
                    event.setAvailableAt(leaseUntil);
                    return new OutboxEventClaim(
                            event.getId(),
                            event.getEventType(),
                            event.getPayload(),
                            leaseUntil
                    );
                })
                .toList();
    }

    /**
     * Uses the lease timestamp as a fencing token. A delayed publisher cannot
     * overwrite the result of a later claim after its own lease has expired.
     */
    @Transactional
    public boolean markPublished(OutboxEventClaim claim) {
        return repository.markPublished(
                claim.id(), claim.leaseUntil(), clock.instant()) == 1;
    }

    @Transactional
    public boolean markFailed(OutboxEventClaim claim, Exception exception) {
        Instant now = clock.instant();
        int nextAttempt = repository.findAttemptsById(claim.id()).orElse(MAX_ATTEMPTS - 1) + 1;
        boolean exhausted = nextAttempt >= MAX_ATTEMPTS;
        long backoffSeconds = Math.min(3600L, 5L * (1L << Math.min(nextAttempt - 1, 9)));
        Instant retryAt = exhausted ? now : now.plusSeconds(backoffSeconds);

        return repository.markFailed(
                claim.id(),
                claim.leaseUntil(),
                OutboxStatus.PENDING,
                OutboxStatus.FAILED,
                retryAt,
                safeMessage(exception)
        ) == 1;
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
