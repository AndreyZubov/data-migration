package io.datamigration.core.mapping.transform;

import io.datamigration.core.domain.TransformStep;
import io.datamigration.core.domain.TransformType;
import io.datamigration.core.mapping.Transform;
import java.util.Map;
import java.util.Objects;

/**
 * Maps an input value to an output value via a lookup table.
 *
 * <p>Configuration parameters:
 *
 * <ul>
 *   <li>{@code table} — required {@link Map}; the lookup dictionary
 *   <li>{@code defaultValue} — optional value returned when the input is not in the table
 *   <li>{@code passthrough} — optional {@link Boolean} (default {@code false}); when {@code true}
 *       and the input is not found, the input itself is returned instead of {@code defaultValue}
 * </ul>
 *
 * <p>If the input is {@code null} the result is {@code null}.
 */
public final class LookupTransform implements Transform {

    @Override
    public TransformType type() {
        return TransformType.LOOKUP;
    }

    @Override
    public Object apply(TransformStep step, Object input) {
        Objects.requireNonNull(step, "step");
        if (input == null) {
            return null;
        }
        Object rawTable = step.params().get("table");
        if (!(rawTable instanceof Map<?, ?> table)) {
            throw new IllegalArgumentException("'table' is required and must be a Map");
        }
        Object key = input.toString();
        if (table.containsKey(key)) {
            return table.get(key);
        }
        if (table.containsKey(input)) {
            return table.get(input);
        }
        boolean passthrough = Boolean.TRUE.equals(step.params().get("passthrough"));
        return passthrough ? input : step.params().get("defaultValue");
    }
}
