package io.datamigration.core.domain;

import java.util.Objects;

/**
 * Mapping between a source field and a target field, with an optional transform
 * and an optional default value used when the source value is missing or {@code null}.
 *
 * @param sourceField  source column / key name
 * @param targetField  target field name on the destination object
 * @param transform    optional transform applied to the source value (may be {@code null})
 * @param defaultValue optional default applied when the source value is {@code null} (may be {@code null})
 */
public record FieldMapping(
        String sourceField, String targetField, TransformStep transform, Object defaultValue) {

    public FieldMapping {
        Objects.requireNonNull(sourceField, "sourceField");
        Objects.requireNonNull(targetField, "targetField");
    }

    public static FieldMapping direct(String sourceField, String targetField) {
        return new FieldMapping(sourceField, targetField, null, null);
    }

    public static FieldMapping withTransform(
            String sourceField, String targetField, TransformStep transform) {
        return new FieldMapping(sourceField, targetField, transform, null);
    }
}
