package com.taskforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskforge.integrations")
public record IntegrationProperties(
    RabbitMq rabbitmq,
    Redis redis
) {
    public record RabbitMq(boolean enabled, String exchange) {
    }

    public record Redis(boolean enabled, String statsKey) {
    }
}
