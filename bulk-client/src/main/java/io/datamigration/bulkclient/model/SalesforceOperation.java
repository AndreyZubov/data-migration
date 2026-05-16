package io.datamigration.bulkclient.model;

import io.datamigration.core.domain.Operation;

/** Salesforce string values for Bulk API operations and the mapping from the core {@link Operation}. */
public final class SalesforceOperation {

    private SalesforceOperation() {}

    public static String wireValue(Operation operation) {
        return switch (operation) {
            case INSERT -> "insert";
            case UPDATE -> "update";
            case UPSERT -> "upsert";
            case DELETE -> "delete";
            case HARD_DELETE -> "hardDelete";
            case QUERY -> "query";
            case QUERY_ALL -> "queryAll";
        };
    }

    public static boolean isQuery(Operation operation) {
        return operation == Operation.QUERY || operation == Operation.QUERY_ALL;
    }
}
