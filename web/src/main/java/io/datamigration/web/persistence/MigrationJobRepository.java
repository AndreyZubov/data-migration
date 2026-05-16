package io.datamigration.web.persistence;

import io.datamigration.core.domain.JobStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MigrationJobRepository extends JpaRepository<MigrationJobEntity, String> {

    Page<MigrationJobEntity> findByStatus(JobStatus status, Pageable pageable);

    @Modifying
    @Query(
            "update MigrationJobEntity j set j.status = :status, "
                    + "j.startedAt = case when :status = io.datamigration.core.domain.JobStatus.RUNNING and j.startedAt is null then current_timestamp else j.startedAt end, "
                    + "j.completedAt = case when :status in (io.datamigration.core.domain.JobStatus.COMPLETED, io.datamigration.core.domain.JobStatus.FAILED, io.datamigration.core.domain.JobStatus.CANCELLED) then current_timestamp else j.completedAt end "
                    + "where j.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") JobStatus status);

    @Modifying
    @Query(
            "update MigrationJobEntity j set j.processedRecords = :processed, j.failedRecords = :failed where j.id = :id")
    int updateCounts(
            @Param("id") String id,
            @Param("processed") long processed,
            @Param("failed") long failed);

    List<MigrationJobEntity> findAllByStatusInOrderByPriorityDescCreatedAtAsc(
            List<JobStatus> statuses);
}
