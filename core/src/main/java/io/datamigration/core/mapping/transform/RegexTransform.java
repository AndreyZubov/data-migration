package io.datamigration.core.mapping.transform;

import io.datamigration.core.domain.TransformStep;
import io.datamigration.core.domain.TransformType;
import io.datamigration.core.mapping.Transform;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies a regular expression to the input. Supports two modes:
 *
 * <ul>
 *   <li>{@code replace} (default) — performs {@link Matcher#replaceAll(String)} using {@code
 *       replacement} (default empty string).
 *   <li>{@code extract} — returns the contents of the capture group identified by {@code group}
 *       (default {@code 0} = whole match). If the pattern does not match, returns an empty string.
 * </ul>
 *
 * <p>Configuration parameters:
 *
 * <ul>
 *   <li>{@code pattern} — required {@link String}; the regular expression
 *   <li>{@code mode} — optional {@link String} ({@code "replace"} | {@code "extract"})
 *   <li>{@code replacement} — optional {@link String} (replace mode only)
 *   <li>{@code group} — optional {@link Number} (extract mode only)
 * </ul>
 *
 * <p>If the input is {@code null} the result is {@code null}.
 */
public final class RegexTransform implements Transform {

    @Override
    public TransformType type() {
        return TransformType.REGEX;
    }

    @Override
    public Object apply(TransformStep step, Object input) {
        Objects.requireNonNull(step, "step");
        if (input == null) {
            return null;
        }
        Object rawPattern = step.params().get("pattern");
        if (!(rawPattern instanceof String patternStr) || patternStr.isEmpty()) {
            throw new IllegalArgumentException("'pattern' is required and must be a non-empty String");
        }
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(input.toString());

        String mode = (String) step.params().getOrDefault("mode", "replace");
        return switch (mode) {
            case "replace" -> {
                String replacement = (String) step.params().getOrDefault("replacement", "");
                yield matcher.replaceAll(replacement);
            }
            case "extract" -> {
                int group = ((Number) step.params().getOrDefault("group", 0)).intValue();
                yield matcher.find() ? matcher.group(group) : "";
            }
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }
}
