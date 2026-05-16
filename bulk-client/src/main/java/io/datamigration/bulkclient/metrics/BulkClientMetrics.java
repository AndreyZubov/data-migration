package io.datamigration.bulkclient.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;

/**
 * Micrometer instrumentation for {@link io.datamigration.bulkclient.BulkApiClient}.
 *
 * <p>Published metrics:
 *
 * <ul>
 *   <li>{@code sf.bulk.job.duration} (Timer) — wall-clock time from job creation to terminal state,
 *       tagged by {@code orgId}, {@code operation} and final {@code state}.
 *   <li>{@code sf.bulk.errors} (Counter) — number of Bulk API errors surfaced to callers, tagged
 *       by {@code orgId} and {@code reason}.
 * </ul>
 */
public final class BulkClientMetrics {

    public static final String JOB_DURATION = "sf.bulk.job.duration";
    public static final String ERRORS = "sf.bulk.errors";

    private final MeterRegistry registry;

    public BulkClientMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void recordJobDuration(String orgId, String operation, String state, Duration duration) {
        Timer.builder(JOB_DURATION)
                .tags("orgId", orgId, "operation", operation, "state", state)
                .register(registry)
                .record(duration);
    }

    public void incrementError(String orgId, String reason) {
        Counter.builder(ERRORS)
                .tags("orgId", orgId, "reason", reason)
                .register(registry)
                .increment();
    }
}
