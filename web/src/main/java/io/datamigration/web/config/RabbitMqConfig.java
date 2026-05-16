package io.datamigration.web.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology: a direct exchange {@code migration.jobs} with three queues for low, normal
 * and high job priorities. Routing keys are the lowercased priority bucket names.
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "migration.jobs";
    public static final String QUEUE_HIGH = "migration.jobs.high";
    public static final String QUEUE_NORMAL = "migration.jobs.normal";
    public static final String QUEUE_LOW = "migration.jobs.low";

    public static final String ROUTING_HIGH = "high";
    public static final String ROUTING_NORMAL = "normal";
    public static final String ROUTING_LOW = "low";

    @Bean
    public DirectExchange jobsExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue highPriorityQueue() {
        return new Queue(QUEUE_HIGH, true);
    }

    @Bean
    public Queue normalPriorityQueue() {
        return new Queue(QUEUE_NORMAL, true);
    }

    @Bean
    public Queue lowPriorityQueue() {
        return new Queue(QUEUE_LOW, true);
    }

    @Bean
    public Binding highBinding(Queue highPriorityQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(highPriorityQueue).to(jobsExchange).with(ROUTING_HIGH);
    }

    @Bean
    public Binding normalBinding(Queue normalPriorityQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(normalPriorityQueue).to(jobsExchange).with(ROUTING_NORMAL);
    }

    @Bean
    public Binding lowBinding(Queue lowPriorityQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(lowPriorityQueue).to(jobsExchange).with(ROUTING_LOW);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** Maps a job priority integer to one of the three routing-key buckets. */
    public static String routingKeyFor(int priority) {
        if (priority >= 7) {
            return ROUTING_HIGH;
        }
        if (priority >= 4) {
            return ROUTING_NORMAL;
        }
        return ROUTING_LOW;
    }
}
