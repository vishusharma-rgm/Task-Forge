package com.taskforge.job;

import com.taskforge.config.WorkerProperties;
import com.taskforge.dto.CreateJobRequest;
import com.taskforge.timeline.JobEventType;
import com.taskforge.timeline.JobTimelineService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {
    private final JobRepository jobs;
    private final WorkerProperties properties;
    private final JobTimelineService timeline;
    private final Clock clock;

    public JobService(JobRepository jobs, WorkerProperties properties, JobTimelineService timeline) {
        this.jobs = jobs;
        this.properties = properties;
        this.timeline = timeline;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public Job create(CreateJobRequest request) {
        Instant now = clock.instant();
        Job job = new Job(request.type(), request.priority(), properties.maxAttempts(), request.payload(), now);
        Job saved = jobs.save(job);
        timeline.record(saved, JobEventType.CREATED, null, "Job accepted by API");
        return saved;
    }

    @Transactional(readOnly = true)
    public Job get(UUID id) {
        return jobs.findById(id).orElseThrow(() -> new EntityNotFoundException("Job not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Job> recent(JobStatus status, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 200);
        return jobs.findRecent(status, PageRequest.of(0, pageSize));
    }

    @Transactional
    public Job requeue(UUID id) {
        Job job = get(id);
        if (job.getStatus() == JobStatus.DEAD || job.getStatus() == JobStatus.FAILED) {
            job.releaseForRetry(clock.instant());
            timeline.record(job, JobEventType.REQUEUED, null, "Job manually requeued");
        }
        return job;
    }
}
