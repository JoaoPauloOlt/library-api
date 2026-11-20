package com.jpoltramari.library_api.application.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.jpoltramari.library_api.domain.enums.OutboxStatus;
import com.jpoltramari.library_api.domain.model.OutboxEvent;
import com.jpoltramari.library_api.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository repository;

    private final JsonMapper objectMapper = JsonMapper.builder().addModule(new tools.jackson.datatype.jsr310.JavaTimeModule()).build();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldPersistEventEnvelopeWithPendingStatus() throws Exception {
        OutboxEventService service = new OutboxEventService(repository, objectMapper, clock);
        Map<String, Object> data = Map.of("loanId", 42L, "userId", 7L);

        UUID eventId = service.record("LOAN", "42", "loan.returned", data);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        JsonNode payload = objectMapper.readTree(saved.getPayload());

        assertThat(eventId).isEqualTo(saved.getId());
        assertThat(saved.getAggregateType()).isEqualTo("LOAN");
        assertThat(saved.getAggregateId()).isEqualTo("42");
        assertThat(saved.getEventType()).isEqualTo("loan.returned");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getCreatedAt()).isEqualTo(clock.instant());
        assertThat(saved.getAvailableAt()).isEqualTo(clock.instant());
        assertThat(payload.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(payload.get("eventType").asText()).isEqualTo("loan.returned");
        assertThat(payload.get("data").get("loanId").asLong()).isEqualTo(42L);
    }
}
