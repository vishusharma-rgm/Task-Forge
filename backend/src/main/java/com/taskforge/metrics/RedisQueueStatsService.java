package com.taskforge.metrics;

import com.taskforge.config.IntegrationProperties;
import com.taskforge.dto.MetricsResponse;
import com.taskforge.dto.RedisQueueStatsResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "taskforge.integrations.redis", name = "enabled", havingValue = "true")
public class RedisQueueStatsService {
    private final StringRedisTemplate redis;
    private final IntegrationProperties properties;
    private final Clock clock = Clock.systemUTC();

    public RedisQueueStatsService(StringRedisTemplate redis, IntegrationProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void writeSnapshot(MetricsResponse metrics) {
        Map<String, String> values = new HashMap<>();
        metrics.jobsByStatus().forEach((status, count) -> values.put(status, String.valueOf(count)));
        values.put("workerThreads", String.valueOf(metrics.workerThreads()));
        values.put("nodeId", metrics.nodeId());
        values.put("updatedAt", clock.instant().toString());
        redis.opsForHash().putAll(properties.redis().statsKey(), values);
    }

    public RedisQueueStatsResponse readSnapshot() {
        Map<Object, Object> raw = redis.opsForHash().entries(properties.redis().statsKey());
        Map<String, String> values = new HashMap<>();
        raw.forEach((key, value) -> values.put(String.valueOf(key), String.valueOf(value)));
        Instant updatedAt = values.containsKey("updatedAt") ? Instant.parse(values.get("updatedAt")) : null;
        return new RedisQueueStatsResponse(properties.redis().statsKey(), values, updatedAt);
    }
}
