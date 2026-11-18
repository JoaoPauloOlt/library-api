package com.jpoltramari.library_api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void shouldDeclareDurableSharedEventsExchange() {
        TopicExchange exchange = config.libraryEventsExchange();

        assertEquals(RabbitMQConfig.EVENTS_EXCHANGE, exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void shouldDeclareDurableSharedDeadLetterExchange() {
        TopicExchange exchange = config.libraryDeadLetterExchange();

        assertEquals(RabbitMQConfig.DEAD_LETTER_EXCHANGE, exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }
}
