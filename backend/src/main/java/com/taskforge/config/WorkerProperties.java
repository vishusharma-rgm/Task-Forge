package com.taskforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskforge.worker")
public record WorkerProperties(
    boolean enabled,
    String nodeId,
    long pollIntervalMs,
    int batchSize,
    int threadCount,
    long staleJobTimeoutSeconds,
    int maxAttempts
) {
}
