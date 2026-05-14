package io.datamigration.core.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Snapshot of a job's progress at a point in time.
 *
 * @param jobId             owning job id
 * @param totalRecords      total number of records discovered in the input; {@code -1} if unknown
 * @param processedRecords  number of records read and mapped
 * @param succeededRecords  number of records written successfully to the target
 * @param failedRecords     number of records rejected by validation or by the target API
 * @param updatedAt         timestamp this snapshot was taken
 */
public record JobProgress(
        String jobId,
        long totalRecords,
        long processedRecords,
        long succeededRecords,
        long failedRecords,
        Instant updatedAt) {

    public JobProgress {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (processedRecords < 0 || succeededRecords < 0 || failedRecords < 0) {
            throw new IllegalArgumentException("Counters must be non-negative");
        }
    }

    public double completionRatio() {
        if (totalRecords <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) processedRecords / (double) totalRecords);
    }
}
