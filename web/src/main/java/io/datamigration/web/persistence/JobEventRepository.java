package io.datamigration.web.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobEventRepository extends JpaRepository<JobEventEntity, Long> {
    List<JobEventEntity> findByJobIdOrderByOccurredAtAsc(String jobId);
}
