package com.taskforge.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "taskforge.integrations.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMqConfig {
    @Bean
    TopicExchange jobEventsExchange(IntegrationProperties properties) {
        return new TopicExchange(properties.rabbitmq().exchange(), true, false);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
