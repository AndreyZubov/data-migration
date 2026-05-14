package io.datamigration.core.validation;

import java.util.List;
import java.util.Objects;

/**
 * Definition of a single field in a {@link TargetSchema}.
 *
 * @param name             field name as it appears on the target object
 * @param type             logical type
 * @param required         whether the field is mandatory
 * @param picklistValues   allowed values when {@code type == PICKLIST}; ignored otherwise
 * @param maxLength        maximum length for {@code STRING}-like types; {@code -1} for unbounded
 */
public record FieldDefinition(
        String name, FieldType type, boolean required, List<String> picklistValues, int maxLength) {

    public FieldDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        picklistValues = picklistValues == null ? List.of() : List.copyOf(picklistValues);
        if (type == FieldType.PICKLIST && picklistValues.isEmpty()) {
            throw new IllegalArgumentException("PICKLIST field requires at least one allowed value");
        }
    }

    public static FieldDefinition required(String name, FieldType type) {
        return new FieldDefinition(name, type, true, List.of(), -1);
    }

    public static FieldDefinition optional(String name, FieldType type) {
        return new FieldDefinition(name, type, false, List.of(), -1);
    }

    public static FieldDefinition picklist(String name, boolean required, List<String> values) {
        return new FieldDefinition(name, FieldType.PICKLIST, required, values, -1);
    }
}
