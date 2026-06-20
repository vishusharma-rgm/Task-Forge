package com.taskforge.worker;

import com.taskforge.config.WorkerProperties;
import com.taskforge.dto.WorkerHeartbeatResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerHeartbeatService {
    private final WorkerHeartbeatRepository heartbeats;
    private final WorkerProperties properties;
    private final Clock clock = Clock.systemUTC();

    public WorkerHeartbeatService(WorkerHeartbeatRepository heartbeats, WorkerProperties properties) {
        this.heartbeats = heartbeats;
        this.properties = properties;
    }

    @Transactional
    public void beat(String status, int lastClaimedJobs) {
        Instant now = clock.instant();
        WorkerHeartbeat heartbeat = heartbeats.findById(properties.nodeId())
            .orElseGet(() -> new WorkerHeartbeat(properties.nodeId(), status, properties.threadCount(), lastClaimedJobs, now));
        heartbeat.beat(status, properties.threadCount(), lastClaimedJobs, now);
        heartbeats.save(heartbeat);
    }

    @Transactional(readOnly = true)
    public List<WorkerHeartbeatResponse> list() {
        Instant now = clock.instant();
        return heartbeats.findAllByOrderByLastSeenAtDesc().stream()
            .map(heartbeat -> WorkerHeartbeatResponse.from(heartbeat, now))
            .toList();
    }
}
