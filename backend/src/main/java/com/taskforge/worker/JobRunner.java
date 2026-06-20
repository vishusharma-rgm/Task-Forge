package com.taskforge.worker;

import com.taskforge.job.Job;
import com.taskforge.job.JobRepository;
import com.taskforge.job.JobStatus;
import com.taskforge.timeline.JobEventType;
import com.taskforge.timeline.JobTimelineService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobRunner {
    private static final Logger log = LoggerFactory.getLogger(JobRunner.class);

    private final JobRepository jobs;
    private final JobProcessor processor;
    private final JobTimelineService timeline;
    private final Clock clock = Clock.systemUTC();

    public JobRunner(JobRepository jobs, JobProcessor processor, JobTimelineService timeline) {
        this.jobs = jobs;
        this.processor = processor;
        this.timeline = timeline;
    }

    @Transactional
    public void run(UUID jobId) {
        Job job = jobs.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != JobStatus.RUNNING) {
            return;
        }

        Instant now = clock.instant();
        String workerId = job.getLockedBy();
        try {
            processor.process(job);
            job.markSucceeded(now);
            timeline.record(job, JobEventType.SUCCEEDED, workerId, "Job completed successfully");
            log.info("job={} status=succeeded", job.getId());
        } catch (RuntimeException failure) {
            Duration delay = retryDelay(job.getAttempts() + 1);
            String message = rootMessage(failure);
            job.markFailed(message, now.plus(delay), now);
            JobEventType eventType = job.getStatus() == JobStatus.DEAD ? JobEventType.DEAD : JobEventType.FAILED;
            timeline.record(job, eventType, workerId, message);
            log.info("job={} status={} attempts={} nextRunAt={}", job.getId(), job.getStatus(), job.getAttempts(), job.getNextRunAt());
        }
    }

    private Duration retryDelay(int attemptNumber) {
        long seconds = Math.min(300, (long) Math.pow(2, attemptNumber) * 5L);
        return Duration.ofSeconds(seconds);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
