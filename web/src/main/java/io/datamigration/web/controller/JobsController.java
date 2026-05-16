package io.datamigration.web.controller;

import io.datamigration.api.JobsApi;
import io.datamigration.api.model.CreateMigrationJobRequest;
import io.datamigration.api.model.MigrationJob;
import io.datamigration.web.mapper.DtoMappers;
import io.datamigration.web.persistence.MigrationJobEntity;
import io.datamigration.web.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobsController implements JobsApi {

    private final JobService service;

    public JobsController(JobService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<MigrationJob> createJob(CreateMigrationJobRequest request) {
        MigrationJobEntity saved = service.create(request);
        return ResponseEntity.status(201).body(DtoMappers.toDto(saved));
    }

    @Override
    public ResponseEntity<MigrationJob> getJob(String id) {
        return service.find(id)
                .map(DtoMappers::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
