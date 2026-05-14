package io.datamigration.core.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A connected Salesforce organization.
 *
 * @param id            stable identifier (e.g. UUID)
 * @param name          human-readable display name
 * @param instanceUrl   Salesforce instance URL (e.g. {@code https://my-org.my.salesforce.com})
 * @param clientId      OAuth client identifier used for this connection
 * @param status        current connectivity status
 * @param createdAt     timestamp of the initial connection
 */
public record Org(
        String id,
        String name,
        String instanceUrl,
        String clientId,
        OrgStatus status,
        Instant createdAt) {

    public Org {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(instanceUrl, "instanceUrl");
        Objects.requireNonNull(status, "status");
    }
}
