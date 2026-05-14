package io.datamigration.core.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A single migration job: pushes data from a source (Salesforce org or uploaded file) to a target
 * Salesforce org, applying a given {@link Operation} on a given Salesforce object.
 *
 * @param id           stable identifier; may be {@code null} for unpersisted jobs
 * @param name         human-readable label
 * @param sourceOrgId  source Salesforce org id; {@code null} for file-based sources
 * @param targetOrgId  destination Salesforce org id; required
 * @param templateId   optional reference to a {@link MigrationTemplate}
 * @param operation    Bulk API operation to execute
 * @param targetObject Salesforce object API name (e.g. {@code "Contact"})
 * @param status       current lifecycle status
 * @param priority     scheduling priority (higher value = higher priority)
 * @param createdAt    creation timestamp
 * @param startedAt    execution start timestamp; {@code null} until the job starts running
 * @param completedAt  completion timestamp; {@code null} until the job reaches a terminal state
 */
public record MigrationJob(
        String id,
        String name,
        String sourceOrgId,
        String targetOrgId,
        String templateId,
        Operation operation,
        String targetObject,
        JobStatus status,
        int priority,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt) {

    public MigrationJob {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(targetOrgId, "targetOrgId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(targetObject, "targetObject");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
