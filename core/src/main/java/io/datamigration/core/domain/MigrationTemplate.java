package io.datamigration.core.domain;

import java.util.List;
import java.util.Objects;

/**
 * Reusable, named bundle of {@link FieldMapping}s for a given target Salesforce object.
 *
 * @param id           stable identifier; may be {@code null} for unpersisted templates
 * @param name         display name; required
 * @param description  free-form description; may be {@code null}
 * @param targetObject Salesforce object API name (e.g. {@code "Account"})
 * @param mappings     immutable list of field mappings; never {@code null}
 */
public record MigrationTemplate(
        String id,
        String name,
        String description,
        String targetObject,
        List<FieldMapping> mappings) {

    public MigrationTemplate {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(targetObject, "targetObject");
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
    }
}
