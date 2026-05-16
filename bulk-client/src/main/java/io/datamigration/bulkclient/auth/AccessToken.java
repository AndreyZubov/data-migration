package io.datamigration.bulkclient.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A short-lived Salesforce OAuth 2.0 access token together with the org's instance URL.
 *
 * @param value       opaque bearer token string
 * @param instanceUrl base URL of the Salesforce instance ({@code https://<my-org>.my.salesforce.com})
 * @param issuedAt    timestamp when the token was issued
 * @param ttl         expected validity duration; tokens are considered expired after {@code issuedAt + ttl}
 */
public record AccessToken(String value, String instanceUrl, Instant issuedAt, Duration ttl) {

    public AccessToken {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(instanceUrl, "instanceUrl");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(ttl, "ttl");
    }

    public boolean isExpired(Instant now, Duration safetyMargin) {
        return !now.isBefore(issuedAt.plus(ttl).minus(safetyMargin));
    }
}
