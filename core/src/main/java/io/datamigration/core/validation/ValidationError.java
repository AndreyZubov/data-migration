package io.datamigration.core.validation;

import java.util.Objects;

/**
 * A single validation issue reported by {@link SchemaValidator}.
 *
 * @param field   field that failed validation; may be empty for whole-row errors
 * @param code    short machine-readable code (e.g. {@code REQUIRED}, {@code INVALID_TYPE})
 * @param message human-readable description
 */
public record ValidationError(String field, String code, String message) {

    public ValidationError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
