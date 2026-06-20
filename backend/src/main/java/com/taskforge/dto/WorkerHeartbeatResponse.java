package com.taskforge.dto;

import com.taskforge.worker.WorkerHeartbeat;
import java.time.Duration;
import java.time.Instant;

public record WorkerHeartbeatResponse(
    String workerId,
    String status,
    int threadCount,
    int lastClaimedJobs,
    Instant lastSeenAt,
    boolean alive,
    long secondsSinceLastSeen
) {
    public static WorkerHeartbeatResponse from(WorkerHeartbeat heartbeat, Instant now) {
        long ageSeconds = Duration.between(heartbeat.getLastSeenAt(), now).toSeconds();
        return new WorkerHeartbeatResponse(
            heartbeat.getWorkerId(),
            heartbeat.getStatus(),
            heartbeat.getThreadCount(),
            heartbeat.getLastClaimedJobs(),
            heartbeat.getLastSeenAt(),
            ageSeconds <= 15,
            ageSeconds
        );
    }
}
