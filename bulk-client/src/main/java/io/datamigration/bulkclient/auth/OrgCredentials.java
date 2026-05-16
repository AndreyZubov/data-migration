package io.datamigration.bulkclient.auth;

import java.util.Objects;

/**
 * Long-lived OAuth credentials for a single Salesforce org, used to obtain access tokens via the
 * refresh-token flow.
 *
 * @param loginUrl     OAuth endpoint host (e.g. {@code https://login.salesforce.com} or {@code
 *                     https://test.salesforce.com})
 * @param clientId     OAuth consumer key of the Connected App
 * @param clientSecret OAuth consumer secret of the Connected App
 * @param refreshToken long-lived refresh token previously issued for this org
 */
public record OrgCredentials(
        String loginUrl, String clientId, String clientSecret, String refreshToken) {

    public OrgCredentials {
        Objects.requireNonNull(loginUrl, "loginUrl");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(clientSecret, "clientSecret");
        Objects.requireNonNull(refreshToken, "refreshToken");
    }
}
