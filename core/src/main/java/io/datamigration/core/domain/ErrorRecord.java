package io.datamigration.core.domain;

import java.util.Map;
import java.util.Objects;

/**
 * A single error encountered while processing a row of a migration job.
 *
 * @param jobId       owning job id
 * @param rowNumber   1-based row number in the input
 * @param field       field name the error relates to; may be {@code null} for whole-row errors
 * @param errorCode   short machine-readable code (e.g. {@code "REQUIRED"}, {@code "INVALID_TYPE"})
 * @param message     human-readable error message
 * @param rowSnapshot immutable snapshot of the offending row; never {@code null}
 */
public record ErrorRecord(
        String jobId,
        long rowNumber,
        String field,
        String errorCode,
        String message,
        Map<String, Object> rowSnapshot) {

    public ErrorRecord {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(errorCode, "errorCode");
        Objects.requireNonNull(message, "message");
        rowSnapshot = rowSnapshot == null ? Map.of() : Map.copyOf(rowSnapshot);
    }
}
