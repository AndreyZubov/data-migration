package io.datamigration.core.mapping;

import io.datamigration.core.domain.TransformStep;
import io.datamigration.core.domain.TransformType;

/**
 * A pure function that, given a {@link TransformStep} configuration and an input value, produces
 * an output value. Implementations must be deterministic and side-effect free.
 */
public interface Transform {

    /**
     * Apply the transform.
     *
     * @param step  the configuration describing this transform application
     * @param input the raw input value (may be {@code null})
     * @return the transformed value
     */
    Object apply(TransformStep step, Object input);

    /** Marker indicating which {@link TransformType} this transform handles. */
    TransformType type();
}
