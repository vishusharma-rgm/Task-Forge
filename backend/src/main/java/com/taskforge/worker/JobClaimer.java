package com.taskforge.worker;

import com.taskforge.config.WorkerProperties;
import com.taskforge.job.Job;
import com.taskforge.job.JobRepository;
import com.taskforge.job.JobStatus;
import com.taskforge.timeline.JobEventType;
import com.taskforge.timeline.JobTimelineService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobClaimer {
    private final JobRepository jobs;
    private final WorkerProperties properties;
    private final JobTimelineService timeline;
    private final Clock clock = Clock.systemUTC();

    public JobClaimer(JobRepository jobs, WorkerProperties properties, JobTimelineService timeline) {
        this.jobs = jobs;
        this.properties = properties;
        this.timeline = timeline;
    }

    @Transactional
    public List<Job> claimReadyJobs() {
        Instant now = clock.instant();
        List<Job> ready = jobs.findReadyJobsForUpdate(
            List.of(JobStatus.PENDING, JobStatus.FAILED),
            now,
            PageRequest.of(0, properties.batchSize())
        );
        ready.forEach(job -> {
            job.claim(properties.nodeId(), now);
            timeline.record(job, JobEventType.CLAIMED, properties.nodeId(), "Worker claimed job");
        });
        return ready;
    }

    @Transactional
    public void releaseStaleLocks() {
        Instant cutoff = clock.instant().minusSeconds(properties.staleJobTimeoutSeconds());
        jobs.findFirstByStatusAndLockedAtBefore(JobStatus.RUNNING, cutoff)
            .ifPresent(job -> {
                job.releaseStaleLock("Worker lock expired before completion", clock.instant());
                timeline.record(job, JobEventType.STALE_LOCK_RELEASED, properties.nodeId(), "Worker lock expired before completion");
            });
    }
}
