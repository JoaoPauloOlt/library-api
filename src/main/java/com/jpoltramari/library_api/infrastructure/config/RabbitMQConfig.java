package com.jpoltramari.library_api.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the shared messaging topology used by application modules.
 *
 * Queues and bindings are intentionally owned by the modules that consume
 * specific event types. This keeps the shared infrastructure independent
 * from business-specific workflows.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RabbitMQConfig {

    public static final String EVENTS_EXCHANGE = "library.events";
    public static final String DEAD_LETTER_EXCHANGE = "library.events.dlx";

    public static final String CIRCULATION_QUEUE = "library.circulation.events";
    public static final String CIRCULATION_DEAD_LETTER_QUEUE = "library.circulation.events.dlq";
    public static final String FINES_QUEUE = "library.fines.events";
    public static final String FINES_DEAD_LETTER_QUEUE = "library.fines.events.dlq";

    public static final String LOAN_RETURNED_ROUTING_KEY = "loan.returned";
    public static final String FINE_CREATED_ROUTING_KEY = "fine.created";
    public static final String FINE_WAIVED_ROUTING_KEY = "fine.waived";

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";
    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    @Bean
    public TopicExchange libraryEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange libraryDeadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue circulationEventsQueue() {
        return QueueBuilder.durable(CIRCULATION_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARGUMENT, DEAD_LETTER_EXCHANGE)
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARGUMENT, CIRCULATION_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue circulationDeadLetterQueue() {
        return QueueBuilder.durable(CIRCULATION_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Queue finesEventsQueue() {
        return QueueBuilder.durable(FINES_QUEUE)
                .withArgument(DEAD_LETTER_EXCHANGE_ARGUMENT, DEAD_LETTER_EXCHANGE)
                .withArgument(DEAD_LETTER_ROUTING_KEY_ARGUMENT, FINES_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue finesDeadLetterQueue() {
        return QueueBuilder.durable(FINES_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding loanReturnedBinding(Queue circulationEventsQueue, TopicExchange libraryEventsExchange) {
        return BindingBuilder.bind(circulationEventsQueue)
                .to(libraryEventsExchange)
                .with(LOAN_RETURNED_ROUTING_KEY);
    }

    @Bean
    public Binding fineCreatedBinding(Queue finesEventsQueue, TopicExchange libraryEventsExchange) {
        return BindingBuilder.bind(finesEventsQueue)
                .to(libraryEventsExchange)
                .with(FINE_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding fineWaivedBinding(Queue finesEventsQueue, TopicExchange libraryEventsExchange) {
        return BindingBuilder.bind(finesEventsQueue)
                .to(libraryEventsExchange)
                .with(FINE_WAIVED_ROUTING_KEY);
    }

    @Bean
    public Binding circulationDeadLetterBinding(
            Queue circulationDeadLetterQueue,
            TopicExchange libraryDeadLetterExchange
    ) {
        return BindingBuilder.bind(circulationDeadLetterQueue)
                .to(libraryDeadLetterExchange)
                .with(CIRCULATION_DEAD_LETTER_QUEUE);
    }

    @Bean
    public Binding finesDeadLetterBinding(
            Queue finesDeadLetterQueue,
            TopicExchange libraryDeadLetterExchange
    ) {
        return BindingBuilder.bind(finesDeadLetterQueue)
                .to(libraryDeadLetterExchange)
                .with(FINES_DEAD_LETTER_QUEUE);
    }
}
