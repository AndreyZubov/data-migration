package io.datamigration.core.mapping.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.datamigration.core.domain.TransformStep;
import io.datamigration.core.domain.TransformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransformsTest {

    @Test
    void concatJoinsPartsWithSeparator() {
        ConcatTransform transform = new ConcatTransform();
        TransformStep step =
                new TransformStep(
                        TransformType.CONCAT, Map.of("parts", List.of("B", "C"), "separator", "-"));

        assertThat(transform.apply(step, "A")).isEqualTo("A-B-C");
    }

    @Test
    void concatHandlesNullInput() {
        ConcatTransform transform = new ConcatTransform();
        TransformStep step =
                new TransformStep(TransformType.CONCAT, Map.of("parts", List.of("X")));

        assertThat(transform.apply(step, null)).isEqualTo("X");
    }

    @Test
    void splitReturnsRequestedPart() {
        SplitTransform transform = new SplitTransform();
        TransformStep step =
                new TransformStep(TransformType.SPLIT, Map.of("separator", ",", "index", 1));

        assertThat(transform.apply(step, "a,b,c")).isEqualTo("b");
    }

    @Test
    void splitSupportsNegativeIndex() {
        SplitTransform transform = new SplitTransform();
        TransformStep step =
                new TransformStep(TransformType.SPLIT, Map.of("separator", ",", "index", -1));

        assertThat(transform.apply(step, "a,b,c")).isEqualTo("c");
    }

    @Test
    void splitOutOfBoundsReturnsEmpty() {
        SplitTransform transform = new SplitTransform();
        TransformStep step =
                new TransformStep(TransformType.SPLIT, Map.of("separator", ",", "index", 99));

        assertThat(transform.apply(step, "a,b,c")).isEqualTo("");
    }

    @Test
    void splitNullInputReturnsNull() {
        SplitTransform transform = new SplitTransform();
        TransformStep step = new TransformStep(TransformType.SPLIT, Map.of("separator", ","));

        assertThat(transform.apply(step, null)).isNull();
    }

    @Test
    void splitRejectsMissingSeparator() {
        SplitTransform transform = new SplitTransform();
        TransformStep step = new TransformStep(TransformType.SPLIT, Map.of());

        assertThatThrownBy(() -> transform.apply(step, "a,b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lookupResolvesValue() {
        LookupTransform transform = new LookupTransform();
        TransformStep step =
                new TransformStep(
                        TransformType.LOOKUP,
                        Map.of("table", Map.of("M", "Male", "F", "Female"), "defaultValue", "Unknown"));

        assertThat(transform.apply(step, "M")).isEqualTo("Male");
    }

    @Test
    void lookupUsesDefaultWhenAbsent() {
        LookupTransform transform = new LookupTransform();
        TransformStep step =
                new TransformStep(
                        TransformType.LOOKUP, Map.of("table", Map.of("M", "Male"), "defaultValue", "Unknown"));

        assertThat(transform.apply(step, "X")).isEqualTo("Unknown");
    }

    @Test
    void lookupPassthroughReturnsInput() {
        LookupTransform transform = new LookupTransform();
        TransformStep step =
                new TransformStep(
                        TransformType.LOOKUP, Map.of("table", Map.of("M", "Male"), "passthrough", true));

        assertThat(transform.apply(step, "X")).isEqualTo("X");
    }

    @Test
    void regexReplaceRemovesNonDigits() {
        RegexTransform transform = new RegexTransform();
        TransformStep step =
                new TransformStep(
                        TransformType.REGEX,
                        Map.of("pattern", "[^0-9]", "mode", "replace", "replacement", ""));

        assertThat(transform.apply(step, "+1 (415) 555-0123")).isEqualTo("14155550123");
    }

    @Test
    void regexExtractReturnsGroup() {
        RegexTransform transform = new RegexTransform();
        TransformStep step =
                new TransformStep(
                        TransformType.REGEX,
                        Map.of("pattern", "([a-z]+)@", "mode", "extract", "group", 1));

        assertThat(transform.apply(step, "ada@example.com")).isEqualTo("ada");
    }

    @Test
    void regexExtractNoMatchReturnsEmpty() {
        RegexTransform transform = new RegexTransform();
        TransformStep step =
                new TransformStep(TransformType.REGEX, Map.of("pattern", "\\d+", "mode", "extract"));

        assertThat(transform.apply(step, "abc")).isEqualTo("");
    }

    @Test
    void regexNullInputReturnsNull() {
        RegexTransform transform = new RegexTransform();
        TransformStep step = new TransformStep(TransformType.REGEX, Map.of("pattern", "."));

        assertThat(transform.apply(step, null)).isNull();
    }

    @Test
    void regexRejectsUnknownMode() {
        RegexTransform transform = new RegexTransform();
        TransformStep step =
                new TransformStep(TransformType.REGEX, Map.of("pattern", ".", "mode", "bogus"));

        assertThatThrownBy(() -> transform.apply(step, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
