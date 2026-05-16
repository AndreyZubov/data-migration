package io.datamigration.web.service;

import io.datamigration.api.model.CreateMigrationJobRequest;
import io.datamigration.core.domain.JobStatus;
import io.datamigration.core.domain.Operation;
import io.datamigration.web.messaging.JobMessage;
import io.datamigration.web.messaging.JobProducer;
import io.datamigration.web.persistence.MigrationJobEntity;
import io.datamigration.web.persistence.MigrationJobRepository;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final MigrationJobRepository repository;
    private final JobProducer producer;

    public JobService(MigrationJobRepository repository, JobProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Transactional
    public MigrationJobEntity create(CreateMigrationJobRequest request) {
        MigrationJobEntity job = new MigrationJobEntity();
        job.setId(UUID.randomUUID().toString());
        job.setName(request.getName());
        job.setSourceOrgId(request.getSourceOrgId());
        job.setTargetOrgId(request.getTargetOrgId());
        job.setTemplateId(request.getTemplateId());
        job.setOperation(Operation.valueOf(request.getOperation().name()));
        job.setTargetObject(request.getTargetObject());
        job.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        job.setStatus(JobStatus.QUEUED);
        job.setCreatedAt(Instant.now());

        if (request.getInputFileUrl() != null) {
            String url = request.getInputFileUrl().toString();
            URI uri = URI.create(url);
            String path = uri.getPath();
            int slash = path.indexOf('/', 1);
            job.setInputFileKey(slash > 0 ? path.substring(slash + 1) : path.substring(1));
            String key = job.getInputFileKey();
            int dot = key.lastIndexOf('.');
            job.setInputFileFormat(dot > 0 ? key.substring(dot + 1) : "csv");
        }

        MigrationJobEntity saved = repository.save(job);
        producer.publish(new JobMessage(saved.getId(), saved.getPriority()));
        return saved;
    }

    public Optional<MigrationJobEntity> find(String id) {
        return repository.findById(id);
    }
}
