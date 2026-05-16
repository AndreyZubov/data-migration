package io.datamigration.bulkclient;

import io.datamigration.bulkclient.model.JobState;

/**
 * Result of a completed Bulk API 2.0 ingest job.
 *
 * @param jobId               Salesforce-assigned job id
 * @param finalState          terminal job state ({@code JOB_COMPLETE}, {@code FAILED} or {@code ABORTED})
 * @param processedRecords    number of records Salesforce processed
 * @param failedRecords       number of records rejected by Salesforce
 * @param successfulRecordsCsv raw CSV body of successful results (may be empty)
 * @param failedRecordsCsv    raw CSV body of failed results (may be empty)
 */
public record IngestResult(
        String jobId,
        JobState finalState,
        long processedRecords,
        long failedRecords,
        String successfulRecordsCsv,
        String failedRecordsCsv) {}
