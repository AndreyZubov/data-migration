package io.datamigration.batch.job;

import io.datamigration.batch.model.ProcessedRow;
import io.datamigration.batch.spi.ErrorSink;
import io.datamigration.batch.spi.JobStatusUpdater;
import io.datamigration.bulkclient.BulkApiClient;
import io.datamigration.core.domain.FieldMapping;
import io.datamigration.core.domain.Operation;
import io.datamigration.core.format.RecordStream;
import io.datamigration.core.mapping.Mapper;
import io.datamigration.core.validation.SchemaValidator;
import io.datamigration.core.validation.TargetSchema;
import io.minio.MinioClient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration for the migration job. The job has two steps:
 *
 * <ol>
 *   <li><b>prepareStep</b> — chunk-oriented reader/processor/writer. Reads input from MinIO,
 *       applies mappings and validation, accumulates valid rows in the job execution context and
 *       persists invalid rows via the supplied {@link ErrorSink}.
 *   <li><b>submitStep</b> — tasklet that submits accumulated valid rows to Salesforce as a single
 *       Bulk API job and updates the migration job status via {@link JobStatusUpdater}.
 * </ol>
 *
 * <p>The accompanying "4 logical stages" described in the implementation plan — {@code read →
 * map → validate → write} — collapse into the prepare step's reader / processor / writer plus
 * the submit tasklet.
 */
@Configuration
public class MigrationBatchConfig {

    public static final String JOB_NAME = "migrationJob";
    public static final int CHUNK_SIZE = 10_000;

    @Bean
    public Mapper mapper() {
        return new Mapper();
    }

    @Bean
    public SchemaValidator schemaValidator() {
        return new SchemaValidator();
    }

    @Bean
    public Job migrationJob(
            JobRepository jobRepository, Step prepareStep, Step submitStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(prepareStep).next(submitStep).build();
    }

    @Bean
    public Step prepareStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            MinioRecordReader reader,
            ItemProcessor<Map<String, Object>, ProcessedRow> processor,
            ItemWriter<ProcessedRow> writer) {
        return new StepBuilder("prepareStep", jobRepository)
                .<Map<String, Object>, ProcessedRow>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Step submitStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            BulkSubmitTasklet tasklet) {
        return new StepBuilder("submitStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public MinioRecordReader minioRecordReader(
            MinioClient minioClient,
            @Value("#{jobParameters['" + JobParametersKeys.BUCKET + "']}") String bucket,
            @Value("#{jobParameters['" + JobParametersKeys.KEY + "']}") String key,
            @Value("#{jobParameters['" + JobParametersKeys.FORMAT + "']}") String format) {
        return new MinioRecordReader(
                minioClient, bucket, key, RecordStream.Format.fromExtension(format));
    }

    @Bean
    @StepScope
    public ItemProcessor<Map<String, Object>, ProcessedRow> mappingProcessor(
            Mapper mapper, SchemaValidator validator) {
        // For MVP: no per-job mappings or schema wired through job parameters. The processor
        // emits each row unchanged through an empty mapping list, with an empty schema that
        // accepts anything. Web layer extensions will supply real mappings + schema in a future
        // iteration.
        return new MappingProcessor(mapper, validator, List.<FieldMapping>of(), emptySchema());
    }

    @Bean
    @StepScope
    public ItemWriter<ProcessedRow> migrationItemWriter(
            ErrorSink errorSink,
            @Value("#{jobParameters['" + JobParametersKeys.JOB_ID + "']}") String jobId) {
        return new MigrationItemWriter(errorSink, jobId);
    }

    @Bean
    @StepScope
    public BulkSubmitTasklet bulkSubmitTasklet(
            BulkApiClient bulkApiClient,
            JobStatusUpdater statusUpdater,
            @Value("#{jobParameters['" + JobParametersKeys.JOB_ID + "']}") String jobId,
            @Value("#{jobParameters['" + JobParametersKeys.ORG_ID + "']}") String orgId,
            @Value("#{jobParameters['" + JobParametersKeys.TARGET_OBJECT + "']}") String targetObject,
            @Value("#{jobParameters['" + JobParametersKeys.OPERATION + "']}") String operation,
            @Value("#{jobParameters['" + JobParametersKeys.EXTERNAL_ID_FIELD + "']}") String externalIdField,
            @Value("#{jobParameters['" + JobParametersKeys.HEADERS_CSV + "']}") String headersCsv) {
        return new BulkSubmitTasklet(
                bulkApiClient,
                statusUpdater,
                jobId,
                orgId,
                targetObject,
                Operation.valueOf(operation),
                externalIdField == null || externalIdField.isBlank() ? null : externalIdField,
                Arrays.asList(headersCsv.split(",")));
    }

    private static TargetSchema emptySchema() {
        return new TargetSchema("__none__", Map.of());
    }
}
