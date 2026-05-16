package io.datamigration.batch.model;

import io.datamigration.core.validation.ValidationError;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A row that was rejected by the validator; contains the failure list. */
public record InvalidRow(long rowNumber, Map<String, Object> data, List<ValidationError> errors)
        implements ProcessedRow {

    public InvalidRow {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(errors, "errors");
        data = Map.copyOf(data);
        errors = List.copyOf(errors);
    }
}
