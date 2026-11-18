package com.jpoltramari.library_api.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpoltramari.library_api.domain.enums.OutboxStatus;
import com.jpoltramari.library_api.domain.model.OutboxEvent;
import com.jpoltramari.library_api.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Must participate in the business transaction that produced the event.
     * The event is therefore committed or rolled back with the aggregate change.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID record(String aggregateType, String aggregateId, String eventType, Object data) {
        Instant now = clock.instant();
        UUID eventId = UUID.randomUUID();

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("aggregateType", aggregateType);
        envelope.put("aggregateId", aggregateId);
        envelope.put("occurredAt", now);
        envelope.put("data", data);

        OutboxEvent event = new OutboxEvent();
        event.setId(eventId);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(serialize(envelope));
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(now);
        event.setAvailableAt(now);

        repository.save(event);
        return eventId;
    }

    private String serialize(Map<String, Object> envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event.", exception);
        }
    }
}
