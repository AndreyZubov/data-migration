package io.datamigration.batch.job;

import io.datamigration.batch.spi.JobStatusUpdater;
import io.datamigration.bulkclient.BulkApiClient;
import io.datamigration.bulkclient.IngestRequest;
import io.datamigration.bulkclient.IngestResult;
import io.datamigration.core.domain.JobStatus;
import io.datamigration.core.domain.Operation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import reactor.core.publisher.Flux;

/**
 * Final step of the migration: takes the accumulated valid rows from the job execution context and
 * submits them to Salesforce as a single Bulk API job via {@link BulkApiClient}.
 */
public final class BulkSubmitTasklet implements Tasklet {

    private final BulkApiClient client;
    private final JobStatusUpdater statusUpdater;
    private final String jobId;
    private final String orgId;
    private final String targetObject;
    private final Operation operation;
    private final String externalIdField;
    private final List<String> headers;

    public BulkSubmitTasklet(
            BulkApiClient client,
            JobStatusUpdater statusUpdater,
            String jobId,
            String orgId,
            String targetObject,
            Operation operation,
            String externalIdField,
            List<String> headers) {
        this.client = client;
        this.statusUpdater = statusUpdater;
        this.jobId = jobId;
        this.orgId = orgId;
        this.targetObject = targetObject;
        this.operation = operation;
        this.externalIdField = externalIdField;
        this.headers = List.copyOf(headers);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext context) {
        var jobExecutionContext =
                context.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        List<Map<String, Object>> validRows =
                (List<Map<String, Object>>)
                        jobExecutionContext.get(MigrationItemWriter.VALID_ROWS_KEY);
        if (validRows == null || validRows.isEmpty()) {
            statusUpdater.updateStatus(jobId, JobStatus.COMPLETED);
            statusUpdater.updateCounts(jobId, 0, 0);
            return RepeatStatus.FINISHED;
        }

        IngestRequest request =
                new IngestRequest(
                        targetObject,
                        operation,
                        externalIdField,
                        headers,
                        Flux.fromIterable(Collections.unmodifiableList(validRows)));

        IngestResult result = client.ingest(orgId, request).block();
        if (result == null) {
            statusUpdater.updateStatus(jobId, JobStatus.FAILED);
            return RepeatStatus.FINISHED;
        }
        statusUpdater.updateCounts(jobId, result.processedRecords(), result.failedRecords());
        statusUpdater.updateStatus(
                jobId,
                result.finalState().isTerminal()
                                && result.finalState()
                                        == io.datamigration.bulkclient.model.JobState.JOB_COMPLETE
                        ? JobStatus.COMPLETED
                        : JobStatus.FAILED);
        return RepeatStatus.FINISHED;
    }
}
