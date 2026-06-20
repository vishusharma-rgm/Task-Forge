package com.taskforge;

import com.taskforge.dto.CreateJobRequest;
import com.taskforge.job.Job;
import com.taskforge.job.JobService;
import com.taskforge.job.JobStatus;
import com.taskforge.job.JobType;
import com.taskforge.worker.JobClaimer;
import com.taskforge.worker.JobRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "taskforge.worker.enabled=false")
class JobLifecycleTest {
    @Autowired
    JobService jobs;

    @Autowired
    JobClaimer claimer;

    @Autowired
    JobRunner runner;

    @Test
    void workerMovesSuccessfulJobThroughLifecycle() {
        Job created = jobs.create(new CreateJobRequest(JobType.EMAIL, 80, "{\"to\":\"student@example.com\"}"));

        Job claimed = claimer.claimReadyJobs().getFirst();
        runner.run(claimed.getId());

        Job completed = jobs.get(created.getId());
        assertThat(completed.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
    }

    @Test
    void failingJobEventuallyMovesToDeadLetterQueue() {
        Job created = jobs.create(new CreateJobRequest(JobType.WEBHOOK, 40, "{\"fail\":true}"));

        for (int i = 0; i < 3; i++) {
            Job claimed = claimer.claimReadyJobs().getFirst();
            runner.run(claimed.getId());
            Job latest = jobs.get(created.getId());
            if (latest.getStatus() == JobStatus.FAILED) {
                jobs.requeue(created.getId());
            }
        }

        assertThat(jobs.get(created.getId()).getStatus()).isEqualTo(JobStatus.DEAD);
    }
}
