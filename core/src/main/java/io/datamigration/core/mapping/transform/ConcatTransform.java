package io.datamigration.core.mapping.transform;

import io.datamigration.core.domain.TransformStep;
import io.datamigration.core.domain.TransformType;
import io.datamigration.core.mapping.Transform;
import java.util.List;
import java.util.Objects;

/**
 * Concatenates the input value with one or more additional parts.
 *
 * <p>Configuration parameters:
 *
 * <ul>
 *   <li>{@code parts} — {@link List} of {@link String} values appended after the input
 *   <li>{@code separator} — optional {@link String} placed between every part (default: empty)
 * </ul>
 */
public final class ConcatTransform implements Transform {

    @Override
    public TransformType type() {
        return TransformType.CONCAT;
    }

    @Override
    public Object apply(TransformStep step, Object input) {
        Objects.requireNonNull(step, "step");
        String separator = (String) step.params().getOrDefault("separator", "");
        Object rawParts = step.params().getOrDefault("parts", List.of());
        if (!(rawParts instanceof List<?> parts)) {
            throw new IllegalArgumentException("'parts' must be a List");
        }

        StringBuilder out = new StringBuilder();
        out.append(input == null ? "" : input.toString());
        for (Object part : parts) {
            if (!out.isEmpty()) {
                out.append(separator);
            }
            out.append(part == null ? "" : part.toString());
        }
        return out.toString();
    }
}
