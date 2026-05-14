package io.datamigration.core.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validates rows against a {@link TargetSchema}. Stateless and thread-safe.
 *
 * <p>Checks performed:
 *
 * <ul>
 *   <li>Required fields are present and non-null
 *   <li>Values match the declared {@link FieldType}
 *   <li>Picklist values are within the allowed set
 *   <li>String length does not exceed {@link FieldDefinition#maxLength()}
 *   <li>Fields not declared in the schema are reported as {@code UNKNOWN_FIELD} (warning-level)
 * </ul>
 */
public final class SchemaValidator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern URL = Pattern.compile("^https?://\\S+$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9()\\-\\s]{5,}$");
    private static final Pattern SALESFORCE_ID = Pattern.compile("^[a-zA-Z0-9]{15}([a-zA-Z0-9]{3})?$");

    public List<ValidationError> validate(TargetSchema schema, Map<String, Object> row) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(row, "row");

        List<ValidationError> errors = new ArrayList<>();

        for (FieldDefinition definition : schema.fields().values()) {
            Object value = row.get(definition.name());
            if (isMissing(value)) {
                if (definition.required()) {
                    errors.add(new ValidationError(
                            definition.name(), "REQUIRED", "Field is required"));
                }
                continue;
            }
            validateValue(definition, value, errors);
        }

        for (String fieldName : row.keySet()) {
            if (!schema.fields().containsKey(fieldName)) {
                errors.add(new ValidationError(
                        fieldName, "UNKNOWN_FIELD", "Field is not defined on the target schema"));
            }
        }

        return errors;
    }

    private boolean isMissing(Object value) {
        return value == null || (value instanceof String s && s.isBlank());
    }

    private void validateValue(
            FieldDefinition definition, Object value, List<ValidationError> errors) {
        String fieldName = definition.name();
        switch (definition.type()) {
            case STRING -> checkLength(definition, value, errors);
            case INTEGER -> checkInteger(fieldName, value, errors);
            case DECIMAL -> checkDecimal(fieldName, value, errors);
            case BOOLEAN -> checkBoolean(fieldName, value, errors);
            case DATE -> checkDate(fieldName, value, errors);
            case DATETIME -> checkDateTime(fieldName, value, errors);
            case EMAIL -> checkPattern(fieldName, value, EMAIL, "INVALID_EMAIL", errors);
            case URL -> checkPattern(fieldName, value, URL, "INVALID_URL", errors);
            case PHONE -> checkPattern(fieldName, value, PHONE, "INVALID_PHONE", errors);
            case PICKLIST -> checkPicklist(definition, value, errors);
            case ID, REFERENCE ->
                    checkPattern(fieldName, value, SALESFORCE_ID, "INVALID_ID", errors);
        }
    }

    private void checkLength(
            FieldDefinition definition, Object value, List<ValidationError> errors) {
        if (definition.maxLength() < 0) {
            return;
        }
        String s = value.toString();
        if (s.length() > definition.maxLength()) {
            errors.add(new ValidationError(
                    definition.name(),
                    "TOO_LONG",
                    "Value length " + s.length() + " exceeds max " + definition.maxLength()));
        }
    }

    private void checkInteger(String field, Object value, List<ValidationError> errors) {
        if (value instanceof Number) {
            return;
        }
        try {
            Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(field, "INVALID_TYPE", "Expected integer"));
        }
    }

    private void checkDecimal(String field, Object value, List<ValidationError> errors) {
        if (value instanceof Number) {
            return;
        }
        try {
            new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(field, "INVALID_TYPE", "Expected decimal"));
        }
    }

    private void checkBoolean(String field, Object value, List<ValidationError> errors) {
        if (value instanceof Boolean) {
            return;
        }
        String s = value.toString().trim().toLowerCase();
        if (!s.equals("true") && !s.equals("false")) {
            errors.add(new ValidationError(field, "INVALID_TYPE", "Expected boolean"));
        }
    }

    private void checkDate(String field, Object value, List<ValidationError> errors) {
        if (value instanceof LocalDate) {
            return;
        }
        try {
            LocalDate.parse(value.toString().trim());
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError(field, "INVALID_TYPE", "Expected ISO-8601 date"));
        }
    }

    private void checkDateTime(String field, Object value, List<ValidationError> errors) {
        if (value instanceof OffsetDateTime) {
            return;
        }
        try {
            OffsetDateTime.parse(value.toString().trim());
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError(field, "INVALID_TYPE", "Expected ISO-8601 date-time"));
        }
    }

    private void checkPattern(
            String field,
            Object value,
            Pattern pattern,
            String code,
            List<ValidationError> errors) {
        if (!pattern.matcher(value.toString().trim()).matches()) {
            errors.add(new ValidationError(field, code, "Value does not match expected pattern"));
        }
    }

    private void checkPicklist(
            FieldDefinition definition, Object value, List<ValidationError> errors) {
        if (!definition.picklistValues().contains(value.toString())) {
            errors.add(new ValidationError(
                    definition.name(),
                    "INVALID_PICKLIST",
                    "Value '" + value + "' not in allowed set " + definition.picklistValues()));
        }
    }
}
