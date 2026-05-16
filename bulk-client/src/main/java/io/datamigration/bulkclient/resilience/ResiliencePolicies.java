package io.datamigration.bulkclient.resilience;

import io.datamigration.bulkclient.BulkApiException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Per-org Resilience4j bundle: retry with exponential backoff, circuit breaker, and bulkhead.
 *
 * <p>Names of underlying registries are derived from {@code orgId} so that each org gets its own
 * circuit breaker, bulkhead and retry instance — failures on one org do not throttle traffic to
 * others.
 */
public final class ResiliencePolicies {

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;

    public ResiliencePolicies() {
        this(defaultRetryConfig(), defaultCircuitBreakerConfig(), defaultBulkheadConfig());
    }

    public ResiliencePolicies(
            RetryConfig retryConfig,
            CircuitBreakerConfig circuitBreakerConfig,
            BulkheadConfig bulkheadConfig) {
        this.retryRegistry = RetryRegistry.of(retryConfig);
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        this.bulkheadRegistry = BulkheadRegistry.of(bulkheadConfig);
    }

    public RetryRegistry retryRegistry() {
        return retryRegistry;
    }

    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return circuitBreakerRegistry;
    }

    public BulkheadRegistry bulkheadRegistry() {
        return bulkheadRegistry;
    }

    /** Operator stack to apply to any {@link Mono} representing a Bulk API call for {@code orgId}. */
    public <T> Function<Mono<T>, Publisher<T>> monoPolicies(String orgId) {
        Retry retry = retryRegistry.retry("retry-" + orgId);
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(orgId);
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(orgId);
        return mono ->
                mono.transformDeferred(BulkheadOperator.of(bulkhead))
                        .transformDeferred(CircuitBreakerOperator.of(breaker))
                        .transformDeferred(RetryOperator.of(retry));
    }

    /** Same stack as {@link #monoPolicies(String)} but for {@link Flux} pipelines. */
    public <T> Function<Flux<T>, Publisher<T>> fluxPolicies(String orgId) {
        Retry retry = retryRegistry.retry("retry-" + orgId);
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(orgId);
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(orgId);
        return flux ->
                flux.transformDeferred(BulkheadOperator.of(bulkhead))
                        .transformDeferred(CircuitBreakerOperator.of(breaker))
                        .transformDeferred(RetryOperator.of(retry));
    }

    private static RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(
                        io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
                                Duration.ofMillis(200), 2.0))
                .retryOnException(ResiliencePolicies::isRetryable)
                .build();
    }

    private static CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }

    private static BulkheadConfig defaultBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(10)
                .maxWaitDuration(Duration.ofSeconds(5))
                .build();
    }

    private static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof IOException) {
            return true;
        }
        if (throwable instanceof BulkApiException ex) {
            int code = ex.statusCode();
            return code >= 500 || code == 408 || code == 429;
        }
        return false;
    }
}
