package com.taskforge.timeline;

import com.taskforge.config.IntegrationProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "taskforge.integrations.rabbitmq", name = "enabled", havingValue = "true")
public class JobEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(JobEventPublisher.class);

    private final RabbitTemplate rabbit;
    private final IntegrationProperties properties;

    public JobEventPublisher(RabbitTemplate rabbit, IntegrationProperties properties) {
        this.rabbit = rabbit;
        this.properties = properties;
    }

    public void publish(JobEvent event) {
        try {
            rabbit.convertAndSend(
                properties.rabbitmq().exchange(),
                "job." + event.getType().name().toLowerCase(),
                Map.of(
                    "jobId", event.getJobId().toString(),
                    "eventType", event.getType().name(),
                    "workerId", event.getWorkerId() == null ? "" : event.getWorkerId(),
                    "attempt", event.getAttempt(),
                    "createdAt", event.getCreatedAt().toString()
                )
            );
        } catch (AmqpException exception) {
            log.warn("RabbitMQ publish failed for job={} event={}", event.getJobId(), event.getType(), exception);
        }
    }
}
