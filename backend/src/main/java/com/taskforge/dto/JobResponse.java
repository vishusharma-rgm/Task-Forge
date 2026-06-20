package com.taskforge.dto;

import com.taskforge.job.Job;
import com.taskforge.job.JobStatus;
import com.taskforge.job.JobType;
import java.time.Instant;
import java.util.UUID;

public record JobResponse(
    UUID id,
    JobType type,
    JobStatus status,
    int priority,
    int attempts,
    int maxAttempts,
    String payload,
    String lastError,
    String lockedBy,
    Instant nextRunAt,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
            job.getId(),
            job.getType(),
            job.getStatus(),
            job.getPriority(),
            job.getAttempts(),
            job.getMaxAttempts(),
            job.getPayload(),
            job.getLastError(),
            job.getLockedBy(),
            job.getNextRunAt(),
            job.getCreatedAt(),
            job.getUpdatedAt(),
            job.getCompletedAt()
        );
    }
}
