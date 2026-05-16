package io.datamigration.web.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorRecordRepository extends JpaRepository<ErrorRecordEntity, Long> {

    Page<ErrorRecordEntity> findByJobId(String jobId, Pageable pageable);
}
