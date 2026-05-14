package io.datamigration.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchemaValidatorTest {

    private final SchemaValidator validator = new SchemaValidator();

    @Test
    void reportsRequiredFieldWhenMissing() {
        TargetSchema schema =
                TargetSchema.of("Contact", List.of(FieldDefinition.required("Email", FieldType.EMAIL)));

        List<ValidationError> errors = validator.validate(schema, Map.of());

        assertThat(errors).singleElement().satisfies(
                e -> {
                    assertThat(e.field()).isEqualTo("Email");
                    assertThat(e.code()).isEqualTo("REQUIRED");
                });
    }

    @Test
    void reportsInvalidEmail() {
        TargetSchema schema =
                TargetSchema.of("Contact", List.of(FieldDefinition.required("Email", FieldType.EMAIL)));

        List<ValidationError> errors = validator.validate(schema, Map.of("Email", "not-an-email"));

        assertThat(errors).extracting(ValidationError::code).containsExactly("INVALID_EMAIL");
    }

    @Test
    void reportsTooLongString() {
        TargetSchema schema =
                TargetSchema.of(
                        "Account",
                        List.of(new FieldDefinition("Name", FieldType.STRING, true, List.of(), 5)));

        List<ValidationError> errors = validator.validate(schema, Map.of("Name", "TooLongValue"));

        assertThat(errors).extracting(ValidationError::code).containsExactly("TOO_LONG");
    }

    @Test
    void acceptsValidPicklist() {
        TargetSchema schema =
                TargetSchema.of(
                        "Lead",
                        List.of(FieldDefinition.picklist("Status", true, List.of("Open", "Closed"))));

        assertThat(validator.validate(schema, Map.of("Status", "Open"))).isEmpty();
    }

    @Test
    void reportsInvalidPicklistValue() {
        TargetSchema schema =
                TargetSchema.of(
                        "Lead",
                        List.of(FieldDefinition.picklist("Status", true, List.of("Open", "Closed"))));

        List<ValidationError> errors = validator.validate(schema, Map.of("Status", "Bogus"));

        assertThat(errors).extracting(ValidationError::code).containsExactly("INVALID_PICKLIST");
    }

    @Test
    void reportsUnknownField() {
        TargetSchema schema =
                TargetSchema.of("Contact", List.of(FieldDefinition.required("Email", FieldType.EMAIL)));

        List<ValidationError> errors =
                validator.validate(schema, Map.of("Email", "a@b.com", "Extra", "x"));

        assertThat(errors).extracting(ValidationError::code).containsExactly("UNKNOWN_FIELD");
    }

    @Test
    void validatesIntegerAndDecimalAndBoolean() {
        TargetSchema schema =
                TargetSchema.of(
                        "Demo",
                        List.of(
                                FieldDefinition.required("Count", FieldType.INTEGER),
                                FieldDefinition.required("Amount", FieldType.DECIMAL),
                                FieldDefinition.required("Active", FieldType.BOOLEAN)));

        assertThat(
                validator.validate(
                        schema, Map.of("Count", "42", "Amount", "3.14", "Active", "true")))
                .isEmpty();
        assertThat(
                validator.validate(
                        schema, Map.of("Count", "x", "Amount", "y", "Active", "maybe")))
                .extracting(ValidationError::code)
                .containsExactlyInAnyOrder("INVALID_TYPE", "INVALID_TYPE", "INVALID_TYPE");
    }

    @Test
    void validatesDateAndDateTime() {
        TargetSchema schema =
                TargetSchema.of(
                        "Demo",
                        List.of(
                                FieldDefinition.required("D", FieldType.DATE),
                                FieldDefinition.required("Dt", FieldType.DATETIME)));

        assertThat(
                validator.validate(
                        schema, Map.of("D", "2026-01-01", "Dt", "2026-01-01T10:00:00+00:00")))
                .isEmpty();
    }

    @Test
    void validatesUrlPhoneAndId() {
        TargetSchema schema =
                TargetSchema.of(
                        "Demo",
                        List.of(
                                FieldDefinition.required("U", FieldType.URL),
                                FieldDefinition.required("P", FieldType.PHONE),
                                FieldDefinition.required("Id", FieldType.ID)));

        assertThat(
                validator.validate(
                        schema,
                        Map.of(
                                "U", "https://example.com/path",
                                "P", "+1 415 555 0123",
                                "Id", "001A0000007zXjK")))
                .isEmpty();
    }
}
