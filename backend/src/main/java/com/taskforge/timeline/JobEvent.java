package com.taskforge.timeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "job_events",
    indexes = @Index(name = "idx_job_events_job_id_created_at", columnList = "job_id, created_at")
)
public class JobEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobEventType type;

    @Column
    private String workerId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JobEvent() {
    }

    public JobEvent(UUID jobId, JobEventType type, String workerId, String message, int attempt, Instant createdAt) {
        this.jobId = jobId;
        this.type = type;
        this.workerId = workerId;
        this.message = message;
        this.attempt = attempt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public JobEventType getType() {
        return type;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getMessage() {
        return message;
    }

    public int getAttempt() {
        return attempt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
