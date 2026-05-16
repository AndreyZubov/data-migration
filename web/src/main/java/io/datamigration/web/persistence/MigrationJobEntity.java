package io.datamigration.web.persistence;

import io.datamigration.core.domain.JobStatus;
import io.datamigration.core.domain.Operation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "migration_job")
public class MigrationJobEntity {

    @Id private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_org_id")
    private String sourceOrgId;

    @Column(name = "target_org_id", nullable = false)
    private String targetOrgId;

    @Column(name = "template_id")
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operation operation;

    @Column(name = "target_object", nullable = false)
    private String targetObject;

    @Column(name = "external_id_field")
    private String externalIdField;

    @Column(name = "input_file_key")
    private String inputFileKey;

    @Column(name = "input_file_format")
    private String inputFileFormat;

    @Column(name = "headers_csv")
    private String headersCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private int priority;

    @Column(name = "processed_records", nullable = false)
    private long processedRecords;

    @Column(name = "failed_records", nullable = false)
    private long failedRecords;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceOrgId() {
        return sourceOrgId;
    }

    public void setSourceOrgId(String sourceOrgId) {
        this.sourceOrgId = sourceOrgId;
    }

    public String getTargetOrgId() {
        return targetOrgId;
    }

    public void setTargetOrgId(String targetOrgId) {
        this.targetOrgId = targetOrgId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(String targetObject) {
        this.targetObject = targetObject;
    }

    public String getExternalIdField() {
        return externalIdField;
    }

    public void setExternalIdField(String externalIdField) {
        this.externalIdField = externalIdField;
    }

    public String getInputFileKey() {
        return inputFileKey;
    }

    public void setInputFileKey(String inputFileKey) {
        this.inputFileKey = inputFileKey;
    }

    public String getInputFileFormat() {
        return inputFileFormat;
    }

    public void setInputFileFormat(String inputFileFormat) {
        this.inputFileFormat = inputFileFormat;
    }

    public String getHeadersCsv() {
        return headersCsv;
    }

    public void setHeadersCsv(String headersCsv) {
        this.headersCsv = headersCsv;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(long processedRecords) {
        this.processedRecords = processedRecords;
    }

    public long getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(long failedRecords) {
        this.failedRecords = failedRecords;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
