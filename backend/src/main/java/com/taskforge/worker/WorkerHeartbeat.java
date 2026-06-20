package com.taskforge.worker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "worker_heartbeats")
public class WorkerHeartbeat {
    @Id
    private String workerId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int threadCount;

    @Column(nullable = false)
    private int lastClaimedJobs;

    @Column(nullable = false)
    private Instant lastSeenAt;

    protected WorkerHeartbeat() {
    }

    public WorkerHeartbeat(String workerId, String status, int threadCount, int lastClaimedJobs, Instant lastSeenAt) {
        this.workerId = workerId;
        this.status = status;
        this.threadCount = threadCount;
        this.lastClaimedJobs = lastClaimedJobs;
        this.lastSeenAt = lastSeenAt;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getStatus() {
        return status;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getLastClaimedJobs() {
        return lastClaimedJobs;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void beat(String status, int threadCount, int lastClaimedJobs, Instant now) {
        this.status = status;
        this.threadCount = threadCount;
        this.lastClaimedJobs = lastClaimedJobs;
        this.lastSeenAt = now;
    }
}
