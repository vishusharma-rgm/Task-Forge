package com.taskforge.dto;

import java.util.Map;

public record MetricsResponse(
    Map<String, Long> jobsByStatus,
    int workerThreads,
    String nodeId
) {
}
