package com.taskforge.worker;

import com.taskforge.job.Job;
import com.taskforge.job.JobType;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class JobProcessor {
    public void process(Job job) {
        sleep(Duration.ofMillis(250));
        if (job.getPayload().contains("\"fail\":true")) {
            throw new IllegalStateException("Payload requested simulated failure");
        }
        if (job.getType() == JobType.DATA_EXPORT) {
            sleep(Duration.ofMillis(400));
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Worker interrupted", interrupted);
        }
    }
}
