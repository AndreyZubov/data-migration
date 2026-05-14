package io.datamigration.core.domain;

/** Salesforce Bulk API operation kinds. */
public enum Operation {
    INSERT,
    UPDATE,
    UPSERT,
    DELETE,
    HARD_DELETE,
    QUERY,
    QUERY_ALL
}
