package io.datamigration.web.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationTemplateRepository
        extends JpaRepository<MigrationTemplateEntity, String> {}
