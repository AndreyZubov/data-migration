package io.datamigration.core.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Declarative description of a single transform applied during field mapping.
 *
 * <p>The {@code params} map carries kind-specific configuration (e.g. a regex pattern,
 * a separator, a lookup table). The shape of the map is interpreted by the corresponding
 * {@code Transform} implementation in {@code io.datamigration.core.mapping.transform}.
 *
 * @param type   transform kind
 * @param params immutable view of configuration parameters; never {@code null}
 */
public record TransformStep(TransformType type, Map<String, Object> params) {

    public TransformStep {
        Objects.requireNonNull(type, "type");
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
