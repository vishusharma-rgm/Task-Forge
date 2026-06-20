package com.taskforge.api;

import com.taskforge.dto.WorkerHeartbeatResponse;
import com.taskforge.worker.WorkerHeartbeatService;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "${FRONTEND_ORIGIN:http://localhost:5173}")
@RestController
@RequestMapping("/api/workers")
public class WorkerController {
    private final WorkerHeartbeatService heartbeats;

    public WorkerController(WorkerHeartbeatService heartbeats) {
        this.heartbeats = heartbeats;
    }

    @GetMapping
    public List<WorkerHeartbeatResponse> list() {
        return heartbeats.list();
    }
}
