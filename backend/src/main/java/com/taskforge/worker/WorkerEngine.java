package com.taskforge.worker;

import com.taskforge.config.WorkerProperties;
import com.taskforge.job.Job;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkerEngine {
    private final JobClaimer claimer;
    private final JobRunner runner;
    private final WorkerHeartbeatService heartbeats;
    private final WorkerProperties properties;
    private final ExecutorService executor;

    public WorkerEngine(JobClaimer claimer, JobRunner runner, WorkerHeartbeatService heartbeats, WorkerProperties properties) {
        this.claimer = claimer;
        this.runner = runner;
        this.heartbeats = heartbeats;
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(properties.threadCount());
    }

    @Scheduled(fixedDelayString = "${taskforge.worker.poll-interval-ms}")
    public void poll() {
        if (!properties.enabled()) {
            return;
        }
        claimer.releaseStaleLocks();
        List<Job> claimed = claimer.claimReadyJobs();
        heartbeats.beat("RUNNING", claimed.size());
        claimed.forEach(job -> executor.submit(() -> runner.run(job.getId())));
    }

    public int threadCount() {
        return properties.threadCount();
    }

    public String nodeId() {
        return properties.nodeId();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

}
