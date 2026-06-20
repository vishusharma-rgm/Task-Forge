package com.taskforge.api;

import com.taskforge.dto.CreateJobRequest;
import com.taskforge.dto.JobEventResponse;
import com.taskforge.dto.JobResponse;
import com.taskforge.job.JobService;
import com.taskforge.job.JobStatus;
import com.taskforge.timeline.JobTimelineService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5173}")
@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService service;
    private final JobTimelineService timeline;

    public JobController(JobService service, JobTimelineService timeline) {
        this.service = service;
        this.timeline = timeline;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobResponse create(@Valid @RequestBody CreateJobRequest request) {
        return JobResponse.from(service.create(request));
    }

    @GetMapping
    public List<JobResponse> recent(
        @RequestParam(required = false) JobStatus status,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return service.recent(status, limit).stream().map(JobResponse::from).toList();
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return JobResponse.from(service.get(id));
    }

    @GetMapping("/{id}/events")
    public List<JobEventResponse> timeline(@PathVariable UUID id) {
        service.get(id);
        return timeline.timeline(id);
    }

    @PostMapping("/{id}/requeue")
    public JobResponse requeue(@PathVariable UUID id) {
        return JobResponse.from(service.requeue(id));
    }
}
