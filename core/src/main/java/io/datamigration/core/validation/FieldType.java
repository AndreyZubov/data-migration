package io.datamigration.core.validation;

/** Logical field types understood by the {@link SchemaValidator}. */
public enum FieldType {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    DATETIME,
    EMAIL,
    URL,
    PHONE,
    PICKLIST,
    ID,
    REFERENCE
}
