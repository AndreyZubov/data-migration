package io.datamigration.bulkclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.datamigration.bulkclient.auth.InMemoryOrgTokenProvider;
import io.datamigration.bulkclient.auth.OrgCredentials;
import io.datamigration.bulkclient.internal.ReactiveBulkApiClient;
import io.datamigration.bulkclient.metrics.BulkClientMetrics;
import io.datamigration.bulkclient.model.JobState;
import io.datamigration.bulkclient.resilience.ResiliencePolicies;
import io.datamigration.core.domain.Operation;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class BulkApiClientWireMockTest {

    private static final String ORG_ID = "org-1";
    private static final String JOB_ID = "750xx000000000AAA";
    private static final String BASE_API = "/services/data/v60.0/jobs/ingest";
    private static final String JOB_API = BASE_API + "/" + JOB_ID;

    private WireMockServer wireMock;
    private BulkApiClient client;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        String baseUrl = "http://localhost:" + wireMock.port();
        WebClient http = WebClient.builder().build();

        InMemoryOrgTokenProvider tokenProvider = new InMemoryOrgTokenProvider(http);
        tokenProvider.register(
                ORG_ID, new OrgCredentials(baseUrl, "client-id", "client-secret", "rt-1"));

        ResiliencePolicies policies =
                new ResiliencePolicies(
                        RetryConfig.custom()
                                .maxAttempts(3)
                                .waitDuration(Duration.ofMillis(10))
                                .retryOnException(
                                        throwable ->
                                                throwable instanceof java.io.IOException
                                                        || (throwable instanceof BulkApiException ex
                                                                && (ex.statusCode() >= 500
                                                                        || ex.statusCode() == 429)))
                                .build(),
                        CircuitBreakerConfig.custom()
                                .slidingWindowSize(20)
                                .failureRateThreshold(99.0f)
                                .build(),
                        BulkheadConfig.custom().maxConcurrentCalls(50).build());

        registry = new SimpleMeterRegistry();
        client =
                new ReactiveBulkApiClient(
                        http,
                        tokenProvider,
                        policies,
                        new BulkClientMetrics(registry),
                        java.time.Clock.systemUTC(),
                        Duration.ofMillis(10));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void happyPathTenThousandRowInsertCompletesInUnder30Seconds() {
        stubTokenEndpoint();
        stubCreateIngestJob(JobState.OPEN);
        stubBatchUpload();
        stubCloseUpload();
        stubGetStatusOnce(JobState.JOB_COMPLETE, 10_000, 0);
        stubSuccessfulResults("sf__Id,Name\n001AAA,row-1\n");
        stubFailedResults("");

        IngestRequest request =
                new IngestRequest(
                        "Account",
                        Operation.INSERT,
                        null,
                        List.of("Name"),
                        Flux.range(0, 10_000)
                                .map(
                                        i -> {
                                            Map<String, Object> row = new HashMap<>();
                                            row.put("Name", "row-" + i);
                                            return row;
                                        }));

        long started = System.nanoTime();
        IngestResult result = client.ingest(ORG_ID, request).block(Duration.ofSeconds(30));
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        assertThat(elapsedMs).isLessThan(30_000);
        assertThat(result).isNotNull();
        assertThat(result.jobId()).isEqualTo(JOB_ID);
        assertThat(result.finalState()).isEqualTo(JobState.JOB_COMPLETE);
        assertThat(result.processedRecords()).isEqualTo(10_000);
        assertThat(result.failedRecords()).isZero();
        assertThat(registry.find("sf.bulk.job.duration").timer()).isNotNull();
    }

    @Test
    void retriesOnTransientServerError() {
        stubTokenEndpoint();
        stubCreateIngestJob(JobState.OPEN);
        stubBatchUpload();
        stubCloseUpload();
        stubSuccessfulResults("");
        stubFailedResults("");

        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API))
                        .inScenario("retry")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse().withStatus(503))
                        .willSetStateTo("recovered"));
        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API))
                        .inScenario("retry")
                        .whenScenarioStateIs("recovered")
                        .willReturn(jobInfoJson(JobState.JOB_COMPLETE, 1, 0)));

        IngestResult result =
                client.ingest(ORG_ID, simpleRequest()).block(Duration.ofSeconds(10));

        assertThat(result).isNotNull();
        assertThat(result.finalState()).isEqualTo(JobState.JOB_COMPLETE);
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(JOB_API)));
    }

    @Test
    void refreshesTokenOnUnauthorized() {
        wireMock.stubFor(
                post(urlEqualTo("/services/oauth2/token"))
                        .inScenario("auth")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(tokenResponse("token-v1"))
                        .willSetStateTo("after-first"));
        wireMock.stubFor(
                post(urlEqualTo("/services/oauth2/token"))
                        .inScenario("auth")
                        .whenScenarioStateIs("after-first")
                        .willReturn(tokenResponse("token-v2")));

        stubCreateIngestJob(JobState.OPEN);
        stubBatchUpload();
        stubCloseUpload();
        stubSuccessfulResults("");
        stubFailedResults("");

        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API))
                        .inScenario("status")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Bearer token-v1"))
                        .willReturn(aResponse().withStatus(401))
                        .willSetStateTo("recovered"));
        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API))
                        .inScenario("status")
                        .whenScenarioStateIs("recovered")
                        .withHeader("Authorization", equalTo("Bearer token-v2"))
                        .willReturn(jobInfoJson(JobState.JOB_COMPLETE, 1, 0)));

        IngestResult result =
                client.ingest(ORG_ID, simpleRequest()).block(Duration.ofSeconds(10));

        assertThat(result).isNotNull();
        assertThat(result.finalState()).isEqualTo(JobState.JOB_COMPLETE);
        wireMock.verify(2, postRequestedFor(urlEqualTo("/services/oauth2/token")));
    }

    @Test
    void pollsUntilJobReachesTerminalStateAfterThreeIterations() {
        stubTokenEndpoint();
        stubCreateIngestJob(JobState.OPEN);
        stubBatchUpload();
        stubCloseUpload();
        stubSuccessfulResults("");
        stubFailedResults("");

        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API))
                        .inScenario("polling")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(jobInfoJson(JobState.IN_PROGRESS, 0, 0))
                        .willSetStateTo("poll-2"));
        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API))
                        .inScenario("polling")
                        .whenScenarioStateIs("poll-2")
                        .willReturn(jobInfoJson(JobState.IN_PROGRESS, 0, 0))
                        .willSetStateTo("poll-3"));
        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API))
                        .inScenario("polling")
                        .whenScenarioStateIs("poll-3")
                        .willReturn(jobInfoJson(JobState.JOB_COMPLETE, 3, 0)));

        StepVerifier.create(client.ingest(ORG_ID, simpleRequest()))
                .expectNextMatches(r -> r.finalState() == JobState.JOB_COMPLETE && r.processedRecords() == 3)
                .verifyComplete();

        wireMock.verify(3, getRequestedFor(urlPathEqualTo(JOB_API)));
    }

    private IngestRequest simpleRequest() {
        Map<String, Object> row = new HashMap<>();
        row.put("Name", "Acme");
        return new IngestRequest(
                "Account", Operation.INSERT, null, List.of("Name"), Flux.just(row));
    }

    private void stubTokenEndpoint() {
        wireMock.stubFor(
                post(urlEqualTo("/services/oauth2/token"))
                        .willReturn(tokenResponse("test-access-token")));
    }

    private com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder tokenResponse(
            String accessToken) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(
                        "{\"access_token\":\""
                                + accessToken
                                + "\",\"instance_url\":\"http://localhost:"
                                + wireMock.port()
                                + "\",\"token_type\":\"Bearer\",\"issued_at\":\"0\"}");
    }

    private void stubCreateIngestJob(JobState state) {
        wireMock.stubFor(
                post(urlEqualTo(BASE_API)).willReturn(jobInfoJson(state, 0, 0)));
    }

    private void stubBatchUpload() {
        wireMock.stubFor(
                put(urlEqualTo(JOB_API + "/batches")).willReturn(aResponse().withStatus(201)));
    }

    private void stubCloseUpload() {
        wireMock.stubFor(
                patch(urlEqualTo(JOB_API))
                        .willReturn(jobInfoJson(JobState.UPLOAD_COMPLETE, 0, 0)));
    }

    private void stubGetStatusOnce(JobState state, long processed, long failed) {
        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API)).willReturn(jobInfoJson(state, processed, failed)));
    }

    private void stubSuccessfulResults(String body) {
        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API + "/successfulResults"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/csv")
                                        .withBody(body)));
    }

    private void stubFailedResults(String body) {
        wireMock.stubFor(
                get(urlPathEqualTo(JOB_API + "/failedResults"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/csv")
                                        .withBody(body)));
    }

    private com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder jobInfoJson(
            JobState state, long processed, long failed) {
        String body =
                "{\"id\":\""
                        + JOB_ID
                        + "\",\"operation\":\"insert\",\"object\":\"Account\",\"state\":\""
                        + state.salesforceValue()
                        + "\",\"numberRecordsProcessed\":"
                        + processed
                        + ",\"numberRecordsFailed\":"
                        + failed
                        + ",\"apiVersion\":\"60.0\"}";
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }
}
