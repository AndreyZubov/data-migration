package io.datamigration.core.domain;

/** Lifecycle status of a {@link MigrationJob}. */
public enum JobStatus {
    PENDING,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
