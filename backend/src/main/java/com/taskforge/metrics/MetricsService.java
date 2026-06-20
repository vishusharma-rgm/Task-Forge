package com.taskforge.metrics;

import com.taskforge.dto.MetricsResponse;
import com.taskforge.job.JobRepository;
import com.taskforge.job.JobStatus;
import com.taskforge.worker.WorkerEngine;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final JobRepository jobs;
    private final WorkerEngine workers;
    private final ObjectProvider<RedisQueueStatsService> redisStats;

    public MetricsService(JobRepository jobs, WorkerEngine workers, ObjectProvider<RedisQueueStatsService> redisStats) {
        this.jobs = jobs;
        this.workers = workers;
        this.redisStats = redisStats;
    }

    public MetricsResponse snapshot() {
        Map<String, Long> counts = Arrays.stream(JobStatus.values())
            .collect(Collectors.toMap(Enum::name, jobs::countByStatus));
        MetricsResponse snapshot = new MetricsResponse(counts, workers.threadCount(), workers.nodeId());
        redisStats.ifAvailable(stats -> stats.writeSnapshot(snapshot));
        return snapshot;
    }
}
