package com.taskforge.job;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select j from Job j
        where j.status in :statuses and j.nextRunAt <= :now
        order by j.priority desc, j.createdAt asc
        """)
    List<Job> findReadyJobsForUpdate(
        @Param("statuses") Collection<JobStatus> statuses,
        @Param("now") Instant now,
        Pageable pageable
    );

    @Query("select j from Job j where (:status is null or j.status = :status) order by j.createdAt desc")
    List<Job> findRecent(@Param("status") JobStatus status, Pageable pageable);

    long countByStatus(JobStatus status);

    Optional<Job> findFirstByStatusAndLockedAtBefore(JobStatus status, Instant cutoff);
}
