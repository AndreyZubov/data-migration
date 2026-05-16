package io.datamigration.bulkclient.auth;

import reactor.core.publisher.Mono;

/**
 * Resolves a current, valid {@link AccessToken} for a given Salesforce org id.
 *
 * <p>Implementations are expected to cache tokens and refresh them transparently when expired.
 */
public interface OrgTokenProvider {

    /** Return a token that is currently valid, refreshing it if necessary. */
    Mono<AccessToken> getToken(String orgId);

    /**
     * Force a refresh and return a new token. Called by {@link
     * io.datamigration.bulkclient.BulkApiClient} when Salesforce responds with 401 and a single
     * silent re-auth is warranted.
     */
    Mono<AccessToken> refresh(String orgId);
}
