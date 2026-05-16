package io.datamigration.batch.job;

import io.datamigration.batch.model.InvalidRow;
import io.datamigration.batch.model.ProcessedRow;
import io.datamigration.batch.model.ValidRow;
import io.datamigration.batch.spi.ErrorSink;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * Writer for the prepare step.
 *
 * <p>For each chunk: valid rows are accumulated into the job-level execution context (so the
 * submit-tasklet can read all of them at the end), and invalid rows are persisted via {@link
 * ErrorSink}.
 *
 * <p>The accumulator is stored as {@code List<Map<String,Object>>} in the job execution context
 * under the key {@link #VALID_ROWS_KEY}.
 */
public final class MigrationItemWriter implements ItemWriter<ProcessedRow> {

    public static final String VALID_ROWS_KEY = "validRows";

    private final ErrorSink errorSink;
    private final String jobId;

    public MigrationItemWriter(ErrorSink errorSink, String jobId) {
        this.errorSink = errorSink;
        this.jobId = jobId;
    }

    @Override
    public void write(Chunk<? extends ProcessedRow> chunk) {
        List<Map<String, Object>> validBatch = new ArrayList<>();
        List<InvalidRow> invalidBatch = new ArrayList<>();
        for (ProcessedRow row : chunk) {
            if (row instanceof ValidRow v) {
                validBatch.add(v.data());
            } else if (row instanceof InvalidRow i) {
                invalidBatch.add(i);
            }
        }
        if (!validBatch.isEmpty()) {
            appendToJobContext(validBatch);
        }
        if (!invalidBatch.isEmpty()) {
            errorSink.persist(jobId, invalidBatch);
        }
    }

    @SuppressWarnings("unchecked")
    private void appendToJobContext(List<Map<String, Object>> batch) {
        org.springframework.batch.core.JobExecution jobExecution =
                StepSynchronizationManager.getContext().getStepExecution().getJobExecution();
        var ctx = jobExecution.getExecutionContext();
        List<Map<String, Object>> accumulator =
                (List<Map<String, Object>>) ctx.get(VALID_ROWS_KEY);
        if (accumulator == null) {
            accumulator = new ArrayList<>();
            ctx.put(VALID_ROWS_KEY, accumulator);
        }
        accumulator.addAll(batch);
    }
}
