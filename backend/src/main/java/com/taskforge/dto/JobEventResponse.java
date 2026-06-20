package com.taskforge.dto;

import com.taskforge.timeline.JobEvent;
import com.taskforge.timeline.JobEventType;
import java.time.Instant;
import java.util.UUID;

public record JobEventResponse(
    UUID id,
    UUID jobId,
    JobEventType type,
    String workerId,
    String message,
    int attempt,
    Instant createdAt
) {
    public static JobEventResponse from(JobEvent event) {
        return new JobEventResponse(
            event.getId(),
            event.getJobId(),
            event.getType(),
            event.getWorkerId(),
            event.getMessage(),
            event.getAttempt(),
            event.getCreatedAt()
        );
    }
}
