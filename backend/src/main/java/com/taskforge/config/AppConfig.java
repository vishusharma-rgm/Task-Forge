package com.taskforge.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({WorkerProperties.class, IntegrationProperties.class})
public class AppConfig {
}
