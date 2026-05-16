package io.datamigration.bulkclient.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** State of a Salesforce Bulk API 2.0 job. */
public enum JobState {
    OPEN("Open"),
    UPLOAD_COMPLETE("UploadComplete"),
    IN_PROGRESS("InProgress"),
    JOB_COMPLETE("JobComplete"),
    ABORTED("Aborted"),
    FAILED("Failed");

    private final String salesforceValue;

    JobState(String salesforceValue) {
        this.salesforceValue = salesforceValue;
    }

    @JsonValue
    public String salesforceValue() {
        return salesforceValue;
    }

    @JsonCreator
    public static JobState from(String value) {
        for (JobState s : values()) {
            if (s.salesforceValue.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown Salesforce job state: " + value);
    }

    public boolean isTerminal() {
        return this == JOB_COMPLETE || this == FAILED || this == ABORTED;
    }
}
