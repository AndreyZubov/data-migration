package io.datamigration.bulkclient.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.datamigration.bulkclient.BulkApiClient;
import io.datamigration.bulkclient.BulkApiException;
import io.datamigration.bulkclient.IngestRequest;
import io.datamigration.bulkclient.IngestResult;
import io.datamigration.bulkclient.auth.AccessToken;
import io.datamigration.bulkclient.auth.OrgTokenProvider;
import io.datamigration.bulkclient.metrics.BulkClientMetrics;
import io.datamigration.bulkclient.model.JobInfo;
import io.datamigration.bulkclient.model.JobState;
import io.datamigration.bulkclient.model.SalesforceOperation;
import io.datamigration.bulkclient.resilience.ResiliencePolicies;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default reactive {@link BulkApiClient} implementation.
 *
 * <p>The class is stateless — all per-org state (tokens, resilience instances) lives in the
 * collaborators passed to the constructor. A single instance can be shared across threads and
 * orgs.
 */
public final class ReactiveBulkApiClient implements BulkApiClient {

    private static final String API_VERSION = "v60.0";
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);

    private final WebClient httpClient;
    private final OrgTokenProvider tokenProvider;
    private final ResiliencePolicies policies;
    private final BulkClientMetrics metrics;
    private final Clock clock;
    private final Duration pollInterval;

    public ReactiveBulkApiClient(
            WebClient httpClient,
            OrgTokenProvider tokenProvider,
            ResiliencePolicies policies,
            BulkClientMetrics metrics) {
        this(httpClient, tokenProvider, policies, metrics, Clock.systemUTC(), DEFAULT_POLL_INTERVAL);
    }

    public ReactiveBulkApiClient(
            WebClient httpClient,
            OrgTokenProvider tokenProvider,
            ResiliencePolicies policies,
            BulkClientMetrics metrics,
            Clock clock,
            Duration pollInterval) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.policies = Objects.requireNonNull(policies, "policies");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.pollInterval = Objects.requireNonNull(pollInterval, "pollInterval");
    }

    @Override
    public Mono<IngestResult> ingest(String orgId, IngestRequest request) {
        Objects.requireNonNull(orgId, "orgId");
        Objects.requireNonNull(request, "request");
        Instant started = clock.instant();

        Mono<IngestResult> pipeline =
                createIngestJob(orgId, request)
                        .flatMap(job ->
                                CsvCodec.encode(request.headers(), request.records())
                                        .flatMap(csv -> uploadBatch(orgId, job.id(), csv))
                                        .thenReturn(job))
                        .flatMap(job -> closeUpload(orgId, job.id()).thenReturn(job))
                        .flatMap(job -> pollUntilTerminal(orgId, ingestPath(job.id())))
                        .flatMap(
                                terminal ->
                                        fetchResultCsv(orgId, ingestPath(terminal.id()) + "/successfulResults")
                                                .zipWith(
                                                        fetchResultCsv(
                                                                orgId,
                                                                ingestPath(terminal.id())
                                                                        + "/failedResults"))
                                                .map(
                                                        tuple ->
                                                                new IngestResult(
                                                                        terminal.id(),
                                                                        terminal.state(),
                                                                        terminal.numberRecordsProcessed(),
                                                                        terminal.numberRecordsFailed(),
                                                                        tuple.getT1(),
                                                                        tuple.getT2())));

        return pipeline
                .doOnSuccess(
                        result ->
                                metrics.recordJobDuration(
                                        orgId,
                                        SalesforceOperation.wireValue(request.operation()),
                                        result.finalState().name(),
                                        Duration.between(started, clock.instant())))
                .doOnError(
                        ex ->
                                metrics.incrementError(
                                        orgId,
                                        ex instanceof BulkApiException bae
                                                ? "http_" + bae.statusCode()
                                                : ex.getClass().getSimpleName()));
    }

    @Override
    public Flux<Map<String, Object>> query(String orgId, String soql) {
        Objects.requireNonNull(orgId, "orgId");
        Objects.requireNonNull(soql, "soql");
        Instant started = clock.instant();

        return createQueryJob(orgId, soql)
                .flatMap(job -> pollUntilTerminal(orgId, queryPath(job.id())))
                .flatMapMany(job -> streamQueryResults(orgId, job.id()))
                .doOnComplete(
                        () ->
                                metrics.recordJobDuration(
                                        orgId,
                                        "query",
                                        JobState.JOB_COMPLETE.name(),
                                        Duration.between(started, clock.instant())))
                .doOnError(
                        ex ->
                                metrics.incrementError(
                                        orgId,
                                        ex instanceof BulkApiException bae
                                                ? "http_" + bae.statusCode()
                                                : ex.getClass().getSimpleName()));
    }

    private Mono<JobInfo> createIngestJob(String orgId, IngestRequest request) {
        CreateIngestJobBody body =
                new CreateIngestJobBody(
                        request.targetObject(),
                        SalesforceOperation.wireValue(request.operation()),
                        request.externalIdField(),
                        "CSV",
                        "LF");
        return resilient(
                orgId,
                token ->
                        httpClient
                                .post()
                                .uri(token.instanceUrl() + ingestRootPath())
                                .headers(headers -> applyAuth(headers, token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(body)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, this::asError)
                                .bodyToMono(JobInfo.class));
    }

    private Mono<Void> uploadBatch(String orgId, String jobId, String csvBody) {
        return resilient(
                orgId,
                token ->
                        httpClient
                                .put()
                                .uri(token.instanceUrl() + ingestPath(jobId) + "/batches")
                                .headers(headers -> applyAuth(headers, token))
                                .contentType(new MediaType("text", "csv"))
                                .accept(MediaType.APPLICATION_JSON)
                                .bodyValue(csvBody)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, this::asError)
                                .toBodilessEntity()
                                .then());
    }

    private Mono<Void> closeUpload(String orgId, String jobId) {
        return resilient(
                orgId,
                token ->
                        httpClient
                                .patch()
                                .uri(token.instanceUrl() + ingestPath(jobId))
                                .headers(headers -> applyAuth(headers, token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("state", JobState.UPLOAD_COMPLETE.salesforceValue()))
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, this::asError)
                                .toBodilessEntity()
                                .then());
    }

    private Mono<JobInfo> pollUntilTerminal(String orgId, String path) {
        return getStatus(orgId, path)
                .flatMap(
                        info ->
                                info.state().isTerminal()
                                        ? Mono.just(info)
                                        : Mono.delay(pollInterval).then(pollUntilTerminal(orgId, path)));
    }

    private Mono<JobInfo> getStatus(String orgId, String path) {
        return resilient(
                orgId,
                token ->
                        httpClient
                                .get()
                                .uri(token.instanceUrl() + path)
                                .headers(headers -> applyAuth(headers, token))
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, this::asError)
                                .bodyToMono(JobInfo.class));
    }

    private Mono<String> fetchResultCsv(String orgId, String path) {
        return resilient(
                orgId,
                token ->
                        httpClient
                                .get()
                                .uri(token.instanceUrl() + path)
                                .headers(headers -> applyAuth(headers, token))
                                .accept(new MediaType("text", "csv"))
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, this::asError)
                                .bodyToMono(String.class)
                                .defaultIfEmpty(""));
    }

    private Mono<JobInfo> createQueryJob(String orgId, String soql) {
        Map<String, Object> body =
                Map.of(
                        "operation", "query",
                        "query", soql,
                        "contentType", "CSV",
                        "lineEnding", "LF");
        return resilient(
                orgId,
                token ->
                        httpClient
                                .post()
                                .uri(token.instanceUrl() + queryRootPath())
                                .headers(headers -> applyAuth(headers, token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(body)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, this::asError)
                                .bodyToMono(JobInfo.class));
    }

    private Flux<Map<String, Object>> streamQueryResults(String orgId, String jobId) {
        return fetchResultsPage(orgId, jobId, null)
                .expand(
                        page ->
                                page.locator() == null || page.locator().isBlank()
                                        ? Mono.empty()
                                        : fetchResultsPage(orgId, jobId, page.locator()))
                .flatMapIterable(QueryResultsPage::rows);
    }

    private Mono<QueryResultsPage> fetchResultsPage(String orgId, String jobId, String locator) {
        String base = queryPath(jobId) + "/results";
        String pathWithLocator =
                locator == null
                        ? base
                        : UriComponentsBuilder.fromPath(base)
                                .queryParam("locator", locator)
                                .build()
                                .toUriString();
        return resilient(
                orgId,
                token ->
                        httpClient
                                .get()
                                .uri(token.instanceUrl() + pathWithLocator)
                                .headers(headers -> applyAuth(headers, token))
                                .accept(new MediaType("text", "csv"))
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, this::asError)
                                .toEntity(String.class)
                                .map(
                                        entity -> {
                                            String body = entity.getBody() == null ? "" : entity.getBody();
                                            String nextLocator =
                                                    entity.getHeaders().getFirst("Sforce-Locator");
                                            return new QueryResultsPage(
                                                    CsvCodec.decode(body),
                                                    "null".equalsIgnoreCase(nextLocator)
                                                            ? null
                                                            : nextLocator);
                                        }));
    }

    private <T> Mono<T> resilient(String orgId, Function<AccessToken, Mono<T>> call) {
        Mono<T> withAuth =
                tokenProvider
                        .getToken(orgId)
                        .flatMap(call)
                        .onErrorResume(
                                BulkApiException.class,
                                ex ->
                                        ex.statusCode() == 401
                                                ? tokenProvider.refresh(orgId).flatMap(call)
                                                : Mono.error(ex));
        return Mono.from(withAuth.transformDeferred(policies.monoPolicies(orgId)));
    }

    private Mono<BulkApiException> asError(org.springframework.web.reactive.function.client.ClientResponse response) {
        int status = response.statusCode().value();
        return response
                .bodyToMono(new ParameterizedTypeReference<java.util.List<SalesforceError>>() {})
                .defaultIfEmpty(java.util.List.of())
                .map(
                        errors -> {
                            String code = errors.isEmpty() ? null : errors.get(0).errorCode();
                            String message =
                                    errors.isEmpty()
                                            ? "Salesforce returned HTTP " + status
                                            : errors.get(0).message();
                            return new BulkApiException(message, status, code, null);
                        });
    }

    private static void applyAuth(HttpHeaders headers, AccessToken token) {
        headers.setBearerAuth(token.value());
    }

    private static String ingestRootPath() {
        return "/services/data/" + API_VERSION + "/jobs/ingest";
    }

    private static String ingestPath(String jobId) {
        return ingestRootPath() + "/" + jobId;
    }

    private static String queryRootPath() {
        return "/services/data/" + API_VERSION + "/jobs/query";
    }

    private static String queryPath(String jobId) {
        return queryRootPath() + "/" + jobId;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CreateIngestJobBody(
            @JsonProperty("object") String object,
            @JsonProperty("operation") String operation,
            @JsonProperty("externalIdFieldName") String externalIdFieldName,
            @JsonProperty("contentType") String contentType,
            @JsonProperty("lineEnding") String lineEnding) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SalesforceError(
            @JsonProperty("errorCode") String errorCode, @JsonProperty("message") String message) {}

    private record QueryResultsPage(java.util.List<Map<String, Object>> rows, String locator) {}
}
