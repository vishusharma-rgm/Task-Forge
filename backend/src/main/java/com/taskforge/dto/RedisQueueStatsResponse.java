package com.taskforge.dto;

import java.time.Instant;
import java.util.Map;

public record RedisQueueStatsResponse(
    String key,
    Map<String, String> values,
    Instant updatedAt
) {
}
