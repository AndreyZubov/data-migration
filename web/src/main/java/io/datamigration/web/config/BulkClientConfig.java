package io.datamigration.web.config;

import io.datamigration.bulkclient.BulkApiClient;
import io.datamigration.bulkclient.auth.InMemoryOrgTokenProvider;
import io.datamigration.bulkclient.auth.OrgCredentials;
import io.datamigration.bulkclient.auth.OrgTokenProvider;
import io.datamigration.bulkclient.internal.ReactiveBulkApiClient;
import io.datamigration.bulkclient.metrics.BulkClientMetrics;
import io.datamigration.bulkclient.resilience.ResiliencePolicies;
import io.datamigration.web.persistence.OrgConnectionEntity;
import io.datamigration.web.persistence.OrgConnectionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Wires the {@link BulkApiClient} and registers org credentials from the DB on startup. */
@Configuration
public class BulkClientConfig {

    private final OrgConnectionRepository orgRepo;
    private final InMemoryOrgTokenProvider tokenProvider;

    public BulkClientConfig(OrgConnectionRepository orgRepo, InMemoryOrgTokenProvider tokenProvider) {
        this.orgRepo = orgRepo;
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public static WebClient bulkClientWebClient() {
        return WebClient.builder().build();
    }

    @Bean
    public static InMemoryOrgTokenProvider tokenProvider(WebClient bulkClientWebClient) {
        return new InMemoryOrgTokenProvider(bulkClientWebClient);
    }

    @Bean
    public static OrgTokenProvider orgTokenProvider(InMemoryOrgTokenProvider provider) {
        return provider;
    }

    @Bean
    public static ResiliencePolicies resiliencePolicies() {
        return new ResiliencePolicies();
    }

    @Bean
    public static BulkClientMetrics bulkClientMetrics(MeterRegistry registry) {
        return new BulkClientMetrics(registry);
    }

    @Bean
    public static BulkApiClient bulkApiClient(
            WebClient bulkClientWebClient,
            OrgTokenProvider tokenProvider,
            ResiliencePolicies policies,
            BulkClientMetrics metrics) {
        return new ReactiveBulkApiClient(bulkClientWebClient, tokenProvider, policies, metrics);
    }

    @PostConstruct
    public void loadOrgs() {
        for (OrgConnectionEntity org : orgRepo.findAll()) {
            tokenProvider.register(
                    org.getId(),
                    new OrgCredentials(
                            org.getLoginUrl(),
                            org.getClientId(),
                            org.getClientSecret(),
                            org.getRefreshToken()));
        }
    }
}
