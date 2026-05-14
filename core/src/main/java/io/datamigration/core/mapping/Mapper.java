package io.datamigration.core.mapping;

import io.datamigration.core.domain.FieldMapping;
import io.datamigration.core.domain.TransformStep;
import io.datamigration.core.domain.TransformType;
import io.datamigration.core.mapping.transform.ConcatTransform;
import io.datamigration.core.mapping.transform.LookupTransform;
import io.datamigration.core.mapping.transform.RegexTransform;
import io.datamigration.core.mapping.transform.SplitTransform;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Applies a list of {@link FieldMapping}s to an input row.
 *
 * <p>The output row preserves field insertion order according to the {@code mappings} list. Source
 * fields that are not referenced by any mapping are not copied to the output.
 *
 * <p>This class is stateless and thread-safe; a single shared instance can be reused.
 */
public final class Mapper {

    private final Map<TransformType, Transform> transforms;

    /** Construct a mapper with the four built-in transforms. */
    public Mapper() {
        this(List.of(
                new ConcatTransform(),
                new SplitTransform(),
                new LookupTransform(),
                new RegexTransform()));
    }

    /**
     * Construct a mapper with a custom set of transforms.
     *
     * @throws IllegalArgumentException if two transforms claim the same {@link TransformType}
     */
    public Mapper(Collection<Transform> transforms) {
        Objects.requireNonNull(transforms, "transforms");
        EnumMap<TransformType, Transform> byType = new EnumMap<>(TransformType.class);
        for (Transform t : transforms) {
            if (byType.putIfAbsent(t.type(), t) != null) {
                throw new IllegalArgumentException("Duplicate transform for type " + t.type());
            }
        }
        this.transforms = Map.copyOf(byType);
    }

    /**
     * Apply {@code mappings} to {@code row} producing a new ordered map.
     *
     * @param mappings non-null, ordered list of mappings
     * @param row      non-null source row (may be empty)
     * @return ordered map keyed by target field name
     */
    public Map<String, Object> map(List<FieldMapping> mappings, Map<String, Object> row) {
        Objects.requireNonNull(mappings, "mappings");
        Objects.requireNonNull(row, "row");
        Map<String, Object> out = new LinkedHashMap<>(mappings.size());
        for (FieldMapping mapping : mappings) {
            Object value = row.get(mapping.sourceField());
            if (value == null) {
                value = mapping.defaultValue();
            }
            TransformStep step = mapping.transform();
            if (step != null) {
                Transform transform = transforms.get(step.type());
                if (transform == null) {
                    throw new IllegalStateException(
                            "No transform registered for type " + step.type());
                }
                value = transform.apply(step, value);
            }
            out.put(mapping.targetField(), value);
        }
        return out;
    }
}
