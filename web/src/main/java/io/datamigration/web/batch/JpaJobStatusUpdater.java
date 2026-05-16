package io.datamigration.web.batch;

import io.datamigration.batch.spi.JobStatusUpdater;
import io.datamigration.core.domain.JobStatus;
import io.datamigration.web.persistence.MigrationJobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaJobStatusUpdater implements JobStatusUpdater {

    private final MigrationJobRepository repository;

    public JpaJobStatusUpdater(MigrationJobRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void updateStatus(String jobId, JobStatus status) {
        repository.updateStatus(jobId, status);
    }

    @Override
    @Transactional
    public void updateCounts(String jobId, long processed, long failed) {
        repository.updateCounts(jobId, processed, failed);
    }
}
