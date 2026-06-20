package com.taskforge.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "jobs",
    indexes = {
        @Index(name = "idx_jobs_ready", columnList = "status, next_run_at, priority"),
        @Index(name = "idx_jobs_status", columnList = "status")
    }
)
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant completedAt;

    @Version
    private long version;

    protected Job() {
    }

    public Job(JobType type, int priority, int maxAttempts, String payload, Instant now) {
        this.type = type;
        this.priority = priority;
        this.maxAttempts = maxAttempts;
        this.payload = payload;
        this.nextRunAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public JobType getType() {
        return type;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getPriority() {
        return priority;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getPayload() {
        return payload;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void claim(String nodeId, Instant now) {
        this.status = JobStatus.RUNNING;
        this.lockedBy = nodeId;
        this.lockedAt = now;
        this.updatedAt = now;
    }

    public void markSucceeded(Instant now) {
        this.status = JobStatus.SUCCEEDED;
        this.lockedBy = null;
        this.lockedAt = null;
        this.lastError = null;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markFailed(String error, Instant nextRunAt, Instant now) {
        this.attempts++;
        this.lastError = error;
        this.lockedBy = null;
        this.lockedAt = null;
        this.updatedAt = now;
        if (this.attempts >= this.maxAttempts) {
            this.status = JobStatus.DEAD;
            this.completedAt = now;
            this.nextRunAt = now;
        } else {
            this.status = JobStatus.FAILED;
            this.nextRunAt = nextRunAt;
        }
    }

    public void releaseForRetry(Instant now) {
        this.status = JobStatus.PENDING;
        this.nextRunAt = now;
        this.updatedAt = now;
    }

    public void releaseStaleLock(String error, Instant now) {
        this.status = JobStatus.PENDING;
        this.lockedBy = null;
        this.lockedAt = null;
        this.lastError = error;
        this.nextRunAt = now;
        this.updatedAt = now;
    }
}
