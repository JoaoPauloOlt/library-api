package com.jpoltramari.library_api.infrastructure.config;

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

    @Bean
    public TopicExchange libraryEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange libraryDeadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }
}
