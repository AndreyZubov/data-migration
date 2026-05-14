package io.datamigration.core.domain;

/** Connectivity status of an {@link Org}. */
public enum OrgStatus {
    CONNECTED,
    DISCONNECTED,
    TOKEN_EXPIRED,
    ERROR
}
