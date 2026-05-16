package io.datamigration.web.messaging;

import io.datamigration.web.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobProducer {

    private final RabbitTemplate rabbit;

    public JobProducer(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publish(JobMessage message) {
        rabbit.convertAndSend(
                RabbitMqConfig.EXCHANGE,
                RabbitMqConfig.routingKeyFor(message.priority()),
                message);
    }
}
