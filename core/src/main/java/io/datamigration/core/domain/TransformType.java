package io.datamigration.core.domain;

/** Built-in transform kinds applicable to a {@link FieldMapping}. */
public enum TransformType {
    CONCAT,
    SPLIT,
    LOOKUP,
    REGEX
}
