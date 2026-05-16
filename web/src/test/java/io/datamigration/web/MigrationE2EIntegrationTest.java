package io.datamigration.web;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end test: spins up Postgres / RabbitMQ / MinIO via Testcontainers and a WireMock-mocked
 * Salesforce instance, then drives the full {@code submit → COMPLETED} flow through the public
 * REST API.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Disabled(
        "Testcontainers 1.21.3 ships a docker-java client that talks Docker Engine API 1.32, but "
                + "Docker 29 dropped support for API < 1.44 (Feb 2026). Re-enable once Testcontainers "
                + "publishes a build with a newer docker-java, or when the host runs Docker 28 or lower.")
class MigrationE2EIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("datamigration")
                    .withUsername("datamigration")
                    .withPassword("datamigration");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Container
    static final MinIOContainer MINIO =
            new MinIOContainer("minio/minio:RELEASE.2024-11-07T00-52-20Z")
                    .withUserName("minioadmin")
                    .withPassword("minioadmin");

    private static WireMockServer wireMock;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("minio.endpoint", MINIO::getS3URL);
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket", () -> "data-migration");
    }

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @LocalServerPort int port;

    @Autowired TestRestTemplate http;
    @Autowired MinioClient minioClient;

    @Test
    void submitJobAndWaitForCompletion() throws Exception {
        stubSalesforce();
        String objectKey = uploadInputCsv();

        String orgId = connectOrg();

        String jobId = createJob(orgId, objectKey);

        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> jobStatus(jobId).equals("COMPLETED"));

        ResponseEntity<Map> response = http.getForEntity(uri("/api/v1/jobs/" + jobId), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "COMPLETED");
    }

    private void stubSalesforce() {
        wireMock.stubFor(
                post(urlEqualTo("/services/oauth2/token"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"access_token\":\"t\",\"instance_url\":\""
                                                        + wireMock.baseUrl()
                                                        + "\",\"token_type\":\"Bearer\",\"issued_at\":\"0\"}")));

        String jobId = "750xx000000000AAA";
        String base = "/services/data/v60.0/jobs/ingest";
        String jobPath = base + "/" + jobId;
        String jobInfoBody =
                "{\"id\":\""
                        + jobId
                        + "\",\"operation\":\"insert\",\"object\":\"Account\",\"state\":\"JobComplete\","
                        + "\"numberRecordsProcessed\":3,\"numberRecordsFailed\":0,\"apiVersion\":\"60.0\"}";
        String openBody = jobInfoBody.replace("JobComplete", "Open");
        String uploadCompleteBody = jobInfoBody.replace("JobComplete", "UploadComplete");

        wireMock.stubFor(
                post(urlEqualTo(base))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(openBody)));
        wireMock.stubFor(put(urlEqualTo(jobPath + "/batches")).willReturn(aResponse().withStatus(201)));
        wireMock.stubFor(
                patch(urlEqualTo(jobPath))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(uploadCompleteBody)));
        wireMock.stubFor(
                get(urlPathEqualTo(jobPath))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(jobInfoBody)));
        wireMock.stubFor(
                get(urlPathEqualTo(jobPath + "/successfulResults"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/csv")
                                        .withBody("sf__Id,Name\n001A,Acme\n")));
        wireMock.stubFor(
                get(urlPathEqualTo(jobPath + "/failedResults"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/csv")
                                        .withBody("")));
    }

    private String uploadInputCsv() throws Exception {
        String csv = "Name\nAcme\nGlobex\nInitech\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        String key = "jobs/test-input.csv";
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("data-migration")
                        .object(key)
                        .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                        .contentType("text/csv")
                        .build());
        return key;
    }

    private String connectOrg() {
        Map<String, Object> body =
                Map.of(
                        "name", "WireMock Org",
                        "instanceUrl", wireMock.baseUrl(),
                        "clientId", "test-client-id",
                        "clientSecret", "test-client-secret",
                        "refreshToken", "test-refresh-token");
        ResponseEntity<Map> response =
                http.postForEntity(uri("/api/v1/orgs"), body, Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return (String) response.getBody().get("id");
    }

    private String createJob(String orgId, String objectKey) {
        Map<String, Object> body =
                Map.of(
                        "name", "E2E test job",
                        "targetOrgId", orgId,
                        "operation", "INSERT",
                        "targetObject", "Account",
                        "priority", 5,
                        "inputFileUrl",
                                URI.create(
                                        MINIO.getS3URL() + "/data-migration/" + objectKey));
        ResponseEntity<Map> response =
                http.postForEntity(uri("/api/v1/jobs"), body, Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return (String) response.getBody().get("id");
    }

    private String jobStatus(String jobId) {
        ResponseEntity<Map> response = http.getForEntity(uri("/api/v1/jobs/" + jobId), Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return "<error>";
        }
        return (String) response.getBody().get("status");
    }

    private String uri(String path) {
        return "http://localhost:" + port + path;
    }
}
