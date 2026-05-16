package io.datamigration.bulkclient.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.datamigration.bulkclient.BulkApiException;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * {@link OrgTokenProvider} implementation that stores per-org {@link OrgCredentials} in memory and
 * uses the OAuth 2.0 refresh-token flow against Salesforce to acquire access tokens.
 *
 * <p>Cached tokens are returned until they fall within {@link #safetyMargin} of expiry, at which
 * point a refresh is issued.
 */
public final class InMemoryOrgTokenProvider implements OrgTokenProvider {

    private static final String TOKEN_ENDPOINT = "/services/oauth2/token";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(110);
    private static final Duration DEFAULT_SAFETY_MARGIN = Duration.ofMinutes(2);

    private final ConcurrentHashMap<String, OrgCredentials> credentials = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AccessToken> tokens = new ConcurrentHashMap<>();
    private final WebClient httpClient;
    private final Clock clock;
    private final Duration ttl;
    private final Duration safetyMargin;

    public InMemoryOrgTokenProvider(WebClient httpClient) {
        this(httpClient, Clock.systemUTC(), DEFAULT_TTL, DEFAULT_SAFETY_MARGIN);
    }

    public InMemoryOrgTokenProvider(
            WebClient httpClient, Clock clock, Duration ttl, Duration safetyMargin) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.safetyMargin = Objects.requireNonNull(safetyMargin, "safetyMargin");
    }

    /** Register credentials for an org so subsequent {@link #getToken(String)} calls can resolve. */
    public void register(String orgId, OrgCredentials orgCredentials) {
        credentials.put(orgId, orgCredentials);
    }

    @Override
    public Mono<AccessToken> getToken(String orgId) {
        AccessToken cached = tokens.get(orgId);
        if (cached != null && !cached.isExpired(clock.instant(), safetyMargin)) {
            return Mono.just(cached);
        }
        return refresh(orgId);
    }

    @Override
    public Mono<AccessToken> refresh(String orgId) {
        OrgCredentials creds = credentials.get(orgId);
        if (creds == null) {
            return Mono.error(new BulkApiException("Unknown org id: " + orgId));
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", creds.clientId());
        form.add("client_secret", creds.clientSecret());
        form.add("refresh_token", creds.refreshToken());

        return httpClient
                .post()
                .uri(creds.loginUrl() + TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response ->
                                response.bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .map(body ->
                                                new BulkApiException(
                                                        "Token refresh failed: HTTP "
                                                                + response.statusCode()
                                                                + " "
                                                                + body,
                                                        response.statusCode().value(),
                                                        null,
                                                        null)))
                .bodyToMono(TokenResponse.class)
                .map(
                        response -> {
                            AccessToken token =
                                    new AccessToken(
                                            response.accessToken(),
                                            response.instanceUrl(),
                                            clock.instant(),
                                            ttl);
                            tokens.put(orgId, token);
                            return token;
                        });
    }

    /** Subset of the Salesforce OAuth 2.0 token response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("instance_url") String instanceUrl,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("issued_at") String issuedAt) {}
}
