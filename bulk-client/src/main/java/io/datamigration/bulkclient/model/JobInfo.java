package io.datamigration.bulkclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of fields returned by the Salesforce Bulk API 2.0 for a job. Fields not declared here are
 * ignored on deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobInfo(
        @JsonProperty("id") String id,
        @JsonProperty("operation") String operation,
        @JsonProperty("object") String object,
        @JsonProperty("state") JobState state,
        @JsonProperty("numberRecordsProcessed") long numberRecordsProcessed,
        @JsonProperty("numberRecordsFailed") long numberRecordsFailed,
        @JsonProperty("apiVersion") String apiVersion) {}
