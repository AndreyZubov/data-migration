package io.datamigration.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.datamigration.core.domain.FieldMapping;
import io.datamigration.core.domain.TransformStep;
import io.datamigration.core.domain.TransformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapperTest {

    private final Mapper mapper = new Mapper();

    @Test
    void mapsDirectFieldsPreservingMappingOrder() {
        Map<String, Object> row = Map.of("first_name", "Ada", "last_name", "Lovelace", "extra", "ignored");

        Map<String, Object> result =
                mapper.map(
                        List.of(
                                FieldMapping.direct("first_name", "FirstName"),
                                FieldMapping.direct("last_name", "LastName")),
                        row);

        assertThat(result)
                .containsExactly(
                        Map.entry("FirstName", "Ada"), Map.entry("LastName", "Lovelace"));
    }

    @Test
    void appliesTransformWhenPresent() {
        TransformStep concat =
                new TransformStep(
                        TransformType.CONCAT, Map.of("parts", List.of("Lovelace"), "separator", " "));
        Map<String, Object> result =
                mapper.map(
                        List.of(FieldMapping.withTransform("first_name", "FullName", concat)),
                        Map.of("first_name", "Ada"));

        assertThat(result).containsEntry("FullName", "Ada Lovelace");
    }

    @Test
    void usesDefaultValueWhenSourceIsMissing() {
        FieldMapping mapping =
                new FieldMapping("missing", "Status", null, "ACTIVE");
        Map<String, Object> result = mapper.map(List.of(mapping), Map.of());

        assertThat(result).containsEntry("Status", "ACTIVE");
    }

    @Test
    void throwsWhenTransformTypeIsNotRegistered() {
        Mapper minimal = new Mapper(List.of());
        FieldMapping mapping =
                FieldMapping.withTransform(
                        "a", "A", new TransformStep(TransformType.CONCAT, Map.of()));

        assertThatThrownBy(() -> minimal.map(List.of(mapping), Map.of("a", "x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONCAT");
    }

    @Test
    void rejectsDuplicateTransforms() {
        assertThatThrownBy(
                () ->
                        new Mapper(
                                List.of(
                                        new io.datamigration.core.mapping.transform.ConcatTransform(),
                                        new io.datamigration.core.mapping.transform.ConcatTransform())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
