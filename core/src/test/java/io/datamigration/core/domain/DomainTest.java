package io.datamigration.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DomainTest {

    @Test
    void jobStatusTerminalFlag() {
        assertThat(JobStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(JobStatus.FAILED.isTerminal()).isTrue();
        assertThat(JobStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(JobStatus.RUNNING.isTerminal()).isFalse();
        assertThat(JobStatus.PENDING.isTerminal()).isFalse();
        assertThat(JobStatus.QUEUED.isTerminal()).isFalse();
    }

    @Test
    void migrationJobRejectsNulls() {
        assertThatThrownBy(
                () ->
                        new MigrationJob(
                                null,
                                null,
                                null,
                                "target",
                                null,
                                Operation.INSERT,
                                "Account",
                                JobStatus.PENDING,
                                0,
                                Instant.EPOCH,
                                null,
                                null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fieldMappingDirectFactory() {
        FieldMapping mapping = FieldMapping.direct("a", "B");

        assertThat(mapping.sourceField()).isEqualTo("a");
        assertThat(mapping.targetField()).isEqualTo("B");
        assertThat(mapping.transform()).isNull();
        assertThat(mapping.defaultValue()).isNull();
    }

    @Test
    void transformStepCopiesParamsDefensively() {
        java.util.Map<String, Object> mutable = new java.util.HashMap<>();
        mutable.put("x", 1);
        TransformStep step = new TransformStep(TransformType.CONCAT, mutable);
        mutable.put("x", 2);

        assertThat(step.params()).containsEntry("x", 1);
    }

    @Test
    void jobProgressCompletionRatioCappedAtOne() {
        JobProgress progress = new JobProgress("job", 100, 150, 100, 0, Instant.EPOCH);
        assertThat(progress.completionRatio()).isEqualTo(1.0);

        JobProgress unknownTotal = new JobProgress("job", -1, 50, 50, 0, Instant.EPOCH);
        assertThat(unknownTotal.completionRatio()).isEqualTo(0.0);

        JobProgress partial = new JobProgress("job", 100, 25, 25, 0, Instant.EPOCH);
        assertThat(partial.completionRatio()).isEqualTo(0.25);
    }

    @Test
    void jobProgressRejectsNegativeCounters() {
        assertThatThrownBy(() -> new JobProgress("job", 100, -1, 0, 0, Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void errorRecordRowSnapshotIsImmutable() {
        java.util.Map<String, Object> snapshot = new java.util.HashMap<>(Map.of("a", 1));
        ErrorRecord record = new ErrorRecord("job", 1L, "f", "CODE", "msg", snapshot);

        assertThatThrownBy(() -> record.rowSnapshot().put("b", 2))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void migrationTemplateAcceptsNullMappings() {
        MigrationTemplate template =
                new MigrationTemplate("id", "name", "desc", "Contact", null);
        assertThat(template.mappings()).isEmpty();
    }

    @Test
    void orgRejectsNullStatus() {
        assertThatThrownBy(
                () ->
                        new Org(
                                "id",
                                "name",
                                "https://example.com",
                                "client",
                                null,
                                Instant.EPOCH))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void operationEnumCoversBulkApiKinds() {
        assertThat(Operation.values()).contains(
                Operation.INSERT,
                Operation.UPDATE,
                Operation.UPSERT,
                Operation.DELETE,
                Operation.HARD_DELETE,
                Operation.QUERY,
                Operation.QUERY_ALL);
    }

    @Test
    void migrationJobRoundTripsThroughCanonicalConstructor() {
        Instant now = Instant.now();
        MigrationJob job =
                new MigrationJob(
                        "id",
                        "Customer import",
                        null,
                        "org-1",
                        null,
                        Operation.UPSERT,
                        "Account",
                        JobStatus.RUNNING,
                        5,
                        now,
                        now,
                        null);

        assertThat(job.priority()).isEqualTo(5);
        assertThat(job.targetObject()).isEqualTo("Account");
        assertThat(job.completedAt()).isNull();
    }

    @Test
    void migrationTemplateMappingsAreImmutable() {
        MigrationTemplate template =
                new MigrationTemplate(
                        "id",
                        "name",
                        null,
                        "Contact",
                        List.of(FieldMapping.direct("a", "B")));

        assertThatThrownBy(() -> template.mappings().add(FieldMapping.direct("x", "Y")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
