package io.datamigration.bulkclient;

import io.datamigration.core.domain.Operation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Flux;

/**
 * Request for a Bulk API 2.0 ingest job.
 *
 * @param targetObject     Salesforce object API name (e.g. {@code "Account"})
 * @param operation        ingest operation kind; must not be {@code QUERY} or {@code QUERY_ALL}
 * @param externalIdField  external id field for {@code UPSERT}; ignored otherwise; may be {@code null}
 * @param headers          ordered CSV column names (target field names)
 * @param records          stream of records to upload; columns must be a subset of {@code headers}
 */
public record IngestRequest(
        String targetObject,
        Operation operation,
        String externalIdField,
        List<String> headers,
        Flux<Map<String, Object>> records) {

    public IngestRequest {
        Objects.requireNonNull(targetObject, "targetObject");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(records, "records");
        if (operation == Operation.QUERY || operation == Operation.QUERY_ALL) {
            throw new IllegalArgumentException("Use BulkApiClient.query for QUERY operations");
        }
        if (operation == Operation.UPSERT
                && (externalIdField == null || externalIdField.isBlank())) {
            throw new IllegalArgumentException("UPSERT requires externalIdField");
        }
        headers = List.copyOf(headers);
    }
}
