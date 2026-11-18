package com.jpoltramari.library_api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void shouldDeclareCirculationQueueWithDeadLetterRouting() {
        Queue queue = config.circulationEventsQueue();

        assertEquals(RabbitMQConfig.CIRCULATION_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMQConfig.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitMQConfig.CIRCULATION_DEAD_LETTER_QUEUE);
    }

    @Test
    void shouldDeclareFinesQueueWithDeadLetterRouting() {
        Queue queue = config.finesEventsQueue();

        assertEquals(RabbitMQConfig.FINES_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMQConfig.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitMQConfig.FINES_DEAD_LETTER_QUEUE);
    }

    @Test
    void shouldRouteLoanReturnedEventsToCirculationQueue() {
        Binding binding = config.loanReturnedBinding(
                config.circulationEventsQueue(),
                config.libraryEventsExchange()
        );

        assertEquals(RabbitMQConfig.CIRCULATION_QUEUE, binding.getDestination());
        assertEquals(RabbitMQConfig.LOAN_RETURNED_ROUTING_KEY, binding.getRoutingKey());
    }

    @Test
    void shouldRouteFineEventsToFinesQueue() {
        Queue queue = config.finesEventsQueue();
        TopicExchange exchange = config.libraryEventsExchange();

        Binding created = config.fineCreatedBinding(queue, exchange);
        Binding waived = config.fineWaivedBinding(queue, exchange);

        assertEquals(RabbitMQConfig.FINES_QUEUE, created.getDestination());
        assertEquals(RabbitMQConfig.FINE_CREATED_ROUTING_KEY, created.getRoutingKey());
        assertEquals(RabbitMQConfig.FINES_QUEUE, waived.getDestination());
        assertEquals(RabbitMQConfig.FINE_WAIVED_ROUTING_KEY, waived.getRoutingKey());
    }

    @Test
    void shouldRouteDeadLettersToDedicatedQueues() {
        TopicExchange deadLetterExchange = config.libraryDeadLetterExchange();

        Binding circulation = config.circulationDeadLetterBinding(
                config.circulationDeadLetterQueue(), deadLetterExchange);
        Binding fines = config.finesDeadLetterBinding(
                config.finesDeadLetterQueue(), deadLetterExchange);

        assertEquals(RabbitMQConfig.CIRCULATION_DEAD_LETTER_QUEUE, circulation.getDestination());
        assertEquals(RabbitMQConfig.CIRCULATION_DEAD_LETTER_QUEUE, circulation.getRoutingKey());
        assertEquals(RabbitMQConfig.FINES_DEAD_LETTER_QUEUE, fines.getDestination());
        assertEquals(RabbitMQConfig.FINES_DEAD_LETTER_QUEUE, fines.getRoutingKey());
    }
}
