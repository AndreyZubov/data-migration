package io.datamigration.bulkclient;

import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive client for the Salesforce Bulk API 2.0.
 *
 * <p>All operations are tied to an {@code orgId} that the configured {@link
 * io.datamigration.bulkclient.auth.OrgTokenProvider} can resolve to an access token and instance
 * URL. Failures from the Salesforce API surface as {@link BulkApiException}; transient errors are
 * retried internally.
 */
public interface BulkApiClient {

    /**
     * Execute a full ingest job: create the job, upload records, close the upload, poll until
     * completion and fetch result CSVs.
     */
    Mono<IngestResult> ingest(String orgId, IngestRequest request);

    /**
     * Submit a Bulk Query job and stream parsed result rows. The returned {@link Flux} polls until
     * the job completes and then emits one row at a time, transparently paging through results via
     * the {@code Sforce-Locator} response header.
     */
    Flux<Map<String, Object>> query(String orgId, String soql);
}
