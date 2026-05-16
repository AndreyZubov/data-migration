package io.datamigration.web.messaging;

import io.datamigration.batch.job.JobParametersKeys;
import io.datamigration.core.domain.JobStatus;
import io.datamigration.web.config.RabbitMqConfig;
import io.datamigration.web.persistence.MigrationJobEntity;
import io.datamigration.web.persistence.MigrationJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Consumes job messages from all three priority queues and triggers the Spring Batch job. */
@Component
public class JobConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobConsumer.class);

    private final JobLauncher jobLauncher;
    private final Job migrationJob;
    private final MigrationJobRepository jobRepository;

    public JobConsumer(
            JobLauncher jobLauncher, Job migrationJob, MigrationJobRepository jobRepository) {
        this.jobLauncher = jobLauncher;
        this.migrationJob = migrationJob;
        this.jobRepository = jobRepository;
    }

    @RabbitListener(
            queues = {RabbitMqConfig.QUEUE_HIGH, RabbitMqConfig.QUEUE_NORMAL, RabbitMqConfig.QUEUE_LOW})
    @Transactional
    public void onMessage(JobMessage message) {
        MigrationJobEntity job =
                jobRepository
                        .findById(message.jobId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Job not found: " + message.jobId()));
        if (job.getStatus().isTerminal()) {
            log.info("Skipping already-terminal job {}", job.getId());
            return;
        }
        jobRepository.updateStatus(job.getId(), JobStatus.RUNNING);
        try {
            JobParameters parameters =
                    new JobParametersBuilder()
                            .addString(JobParametersKeys.JOB_ID, job.getId())
                            .addString(JobParametersKeys.BUCKET, "data-migration")
                            .addString(JobParametersKeys.KEY, job.getInputFileKey())
                            .addString(JobParametersKeys.FORMAT, job.getInputFileFormat())
                            .addString(JobParametersKeys.ORG_ID, job.getTargetOrgId())
                            .addString(JobParametersKeys.TARGET_OBJECT, job.getTargetObject())
                            .addString(JobParametersKeys.OPERATION, job.getOperation().name())
                            .addString(
                                    JobParametersKeys.EXTERNAL_ID_FIELD,
                                    job.getExternalIdField() == null ? "" : job.getExternalIdField())
                            .addString(
                                    JobParametersKeys.HEADERS_CSV,
                                    job.getHeadersCsv() == null ? "" : job.getHeadersCsv())
                            .addLong("runAt", System.currentTimeMillis())
                            .toJobParameters();
            jobLauncher.run(migrationJob, parameters);
        } catch (Exception e) {
            log.error("Migration job {} failed", job.getId(), e);
            jobRepository.updateStatus(job.getId(), JobStatus.FAILED);
        }
    }

}
