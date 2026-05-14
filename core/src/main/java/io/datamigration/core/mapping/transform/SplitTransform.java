package io.datamigration.core.mapping.transform;

import io.datamigration.core.domain.TransformStep;
import io.datamigration.core.domain.TransformType;
import io.datamigration.core.mapping.Transform;
import java.util.Objects;

/**
 * Splits the input string by a separator and returns one of the resulting parts.
 *
 * <p>Configuration parameters:
 *
 * <ul>
 *   <li>{@code separator} — required {@link String}; the delimiter used for splitting
 *   <li>{@code index} — optional {@link Number} (default {@code 0}); the 0-based index of the part
 *       to return; negative values count from the end (e.g. {@code -1} = last part)
 *   <li>{@code limit} — optional {@link Number} (default {@code -1}); see {@link
 *       String#split(String, int)}
 * </ul>
 *
 * <p>If the input is {@code null} the result is {@code null}. If the index is out of bounds the
 * result is an empty string.
 */
public final class SplitTransform implements Transform {

    @Override
    public TransformType type() {
        return TransformType.SPLIT;
    }

    @Override
    public Object apply(TransformStep step, Object input) {
        Objects.requireNonNull(step, "step");
        if (input == null) {
            return null;
        }
        Object rawSeparator = step.params().get("separator");
        if (!(rawSeparator instanceof String separator) || separator.isEmpty()) {
            throw new IllegalArgumentException("'separator' is required and must be a non-empty String");
        }
        int index = ((Number) step.params().getOrDefault("index", 0)).intValue();
        int limit = ((Number) step.params().getOrDefault("limit", -1)).intValue();

        String[] parts = input.toString().split(java.util.regex.Pattern.quote(separator), limit);
        int resolved = index < 0 ? parts.length + index : index;
        if (resolved < 0 || resolved >= parts.length) {
            return "";
        }
        return parts[resolved];
    }
}
