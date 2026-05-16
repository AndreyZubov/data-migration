package io.datamigration.batch.spi;

import io.datamigration.core.domain.JobStatus;

/**
 * Updates the lifecycle status of a migration job. Implemented by the outer persistence module.
 */
public interface JobStatusUpdater {

    void updateStatus(String jobId, JobStatus status);

    void updateCounts(String jobId, long processed, long failed);
}
