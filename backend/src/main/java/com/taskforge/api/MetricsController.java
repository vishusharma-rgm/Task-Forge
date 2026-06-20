package com.taskforge.api;

import com.taskforge.dto.RedisQueueStatsResponse;
import com.taskforge.dto.MetricsResponse;
import com.taskforge.metrics.MetricsService;
import com.taskforge.metrics.RedisQueueStatsService;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5173}")
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    private final MetricsService metrics;
    private final ObjectProvider<RedisQueueStatsService> redisStats;

    public MetricsController(MetricsService metrics, ObjectProvider<RedisQueueStatsService> redisStats) {
        this.metrics = metrics;
        this.redisStats = redisStats;
    }

    @GetMapping
    public MetricsResponse snapshot() {
        return metrics.snapshot();
    }

    @GetMapping("/redis")
    public RedisQueueStatsResponse redisSnapshot() {
        RedisQueueStatsService service = redisStats.getIfAvailable();
        if (service == null) {
            return new RedisQueueStatsResponse("disabled", Map.of(), null);
        }
        return service.readSnapshot();
    }
}
