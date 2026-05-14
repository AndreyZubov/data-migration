package io.datamigration.core.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes the structure of a target Salesforce object for the purposes of pre-flight validation.
 *
 * <p>Field order is preserved according to insertion. Lookup by field name is case-sensitive.
 */
public record TargetSchema(String objectName, Map<String, FieldDefinition> fields) {

    public TargetSchema {
        Objects.requireNonNull(objectName, "objectName");
        Objects.requireNonNull(fields, "fields");
        fields = Map.copyOf(fields);
    }

    public static TargetSchema of(String objectName, List<FieldDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        Map<String, FieldDefinition> map = new LinkedHashMap<>(definitions.size());
        for (FieldDefinition definition : definitions) {
            map.put(definition.name(), definition);
        }
        return new TargetSchema(objectName, map);
    }

    public Optional<FieldDefinition> field(String name) {
        return Optional.ofNullable(fields.get(name));
    }
}
