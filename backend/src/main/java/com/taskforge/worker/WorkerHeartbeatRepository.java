package com.taskforge.worker;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeat, String> {
    List<WorkerHeartbeat> findAllByOrderByLastSeenAtDesc();
}
