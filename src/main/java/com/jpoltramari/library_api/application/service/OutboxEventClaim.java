package com.jpoltramari.library_api.application.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of an outbox event claimed for publication.
 * The lease timestamp fences stale publishers from updating a newer claim.
 */
public record OutboxEventClaim(
        UUID id,
        String eventType,
        String payload,
        Instant leaseUntil
) {
}
