package io.datamigration.batch.model;

import java.util.Map;
import java.util.Objects;

/** A row that passed mapping and validation and is ready for upload. */
public record ValidRow(long rowNumber, Map<String, Object> data) implements ProcessedRow {

    public ValidRow {
        Objects.requireNonNull(data, "data");
        data = Map.copyOf(data);
    }
}
