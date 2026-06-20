package com.taskforge.timeline;

import com.taskforge.dto.JobEventResponse;
import com.taskforge.job.Job;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobTimelineService {
    private final JobEventRepository events;
    private final ObjectProvider<JobEventPublisher> publisher;
    private final Clock clock = Clock.systemUTC();

    public JobTimelineService(JobEventRepository events, ObjectProvider<JobEventPublisher> publisher) {
        this.events = events;
        this.publisher = publisher;
    }

    public void record(Job job, JobEventType type, String workerId, String message) {
        record(job.getId(), type, workerId, message, job.getAttempts());
    }

    public void record(UUID jobId, JobEventType type, String workerId, String message, int attempt) {
        Instant now = clock.instant();
        JobEvent saved = events.save(new JobEvent(jobId, type, workerId, message, attempt, now));
        publisher.ifAvailable(eventPublisher -> eventPublisher.publish(saved));
    }

    @Transactional(readOnly = true)
    public List<JobEventResponse> timeline(UUID jobId) {
        return events.findByJobIdOrderByCreatedAtAsc(jobId).stream()
            .map(JobEventResponse::from)
            .toList();
    }
}
