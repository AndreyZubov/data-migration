package io.datamigration.web.mapper;

import io.datamigration.api.model.JobStatus;
import io.datamigration.api.model.MigrationJob;
import io.datamigration.api.model.Operation;
import io.datamigration.api.model.Org;
import io.datamigration.api.model.OrgStatus;
import io.datamigration.web.persistence.MigrationJobEntity;
import io.datamigration.web.persistence.OrgConnectionEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Maps JPA entities to the generated OpenAPI DTOs. */
public final class DtoMappers {

    private DtoMappers() {}

    public static MigrationJob toDto(MigrationJobEntity e) {
        MigrationJob dto = new MigrationJob();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setSourceOrgId(e.getSourceOrgId());
        dto.setTargetOrgId(e.getTargetOrgId());
        dto.setTemplateId(e.getTemplateId());
        dto.setOperation(Operation.valueOf(e.getOperation().name()));
        dto.setTargetObject(e.getTargetObject());
        dto.setStatus(JobStatus.valueOf(e.getStatus().name()));
        dto.setPriority(e.getPriority());
        dto.setCreatedAt(toOffsetDateTime(e.getCreatedAt()));
        dto.setStartedAt(toOffsetDateTime(e.getStartedAt()));
        dto.setCompletedAt(toOffsetDateTime(e.getCompletedAt()));
        return dto;
    }

    public static Org toDto(OrgConnectionEntity e) {
        Org dto = new Org();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setInstanceUrl(java.net.URI.create(e.getInstanceUrl()));
        dto.setClientId(e.getClientId());
        dto.setStatus(OrgStatus.valueOf(e.getStatus().name()));
        dto.setCreatedAt(toOffsetDateTime(e.getCreatedAt()));
        return dto;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
