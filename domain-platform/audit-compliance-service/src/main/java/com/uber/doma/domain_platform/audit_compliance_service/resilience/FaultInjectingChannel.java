package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic fault-injecting {@link UpstreamAuditChannel} used by chaos
 * tests. The channel simulates two distinct failure modes:
 *
 * <ul>
 *   <li><b>Network latency</b> — every call sleeps for a fixed delay before
 *       returning. Models 500ms round-trip latency as referenced in the
 *       architectural context.</li>
 *   <li><b>Packet loss</b> — a configurable fraction of calls (0.0–1.0)
 *       throw a synthetic {@link SimulatedPartitionException} instead of
 *       returning. Models 20%-30% dropped connections.</li>
 * </ul>
 *
 * <p>The class is thread-safe and tracks the number of injected failures so
 * chaos tests can assert that the fallback layer observed a realistic
 * distribution of faults.</p>
 *
 * <p>This stub is deterministic in the sense that it does not depend on
 * Docker or a Toxiproxy daemon — it lives entirely in-process. The chaos
 * test suite also includes a Testcontainers-backed integration test that
 * uses Toxiproxy for full end-to-end network-level fault injection.</p>
 */
public class FaultInjectingChannel implements UpstreamAuditChannel {

    private final long latencyMillis;
    private final double lossProbability;
    private final UpstreamAuditChannel delegate;

    private final AtomicLong totalAttempts = new AtomicLong(0);
    private final AtomicLong injectedFailures = new AtomicLong(0);

    /**
     * Builds a fault-injecting channel wrapping a delegate.
     *
     * @param latencyMillis   delay applied to every successful call (>= 0)
     * @param lossProbability fraction of calls that fail (0.0–1.0)
     * @param delegate        the channel to invoke after the latency elapses
     */
    public FaultInjectingChannel(final long latencyMillis,
                                 final double lossProbability,
                                 final UpstreamAuditChannel delegate) {
        if (latencyMillis < 0) {
            throw new IllegalArgumentException("latencyMillis must be >= 0");
        }
        if (lossProbability < 0.0 || lossProbability > 1.0) {
            throw new IllegalArgumentException("lossProbability must be in [0.0, 1.0]");
        }
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.latencyMillis = latencyMillis;
        this.lossProbability = lossProbability;
        this.delegate = delegate;
    }

    @Override
    public void persist(final AuditEvent event) {
        totalAttempts.incrementAndGet();
        sleep(latencyMillis);
        if (shouldInjectLoss()) {
            injectedFailures.incrementAndGet();
            throw new SimulatedPartitionException(
                    "synthetic packet loss (prob=" + lossProbability
                            + ") on persist of event " + event.getId());
        }
        delegate.persist(event);
    }

    @Override
    public List<AuditEvent> read(final String actorId) {
        totalAttempts.incrementAndGet();
        sleep(latencyMillis);
        if (shouldInjectLoss()) {
            injectedFailures.incrementAndGet();
            throw new SimulatedPartitionException(
                    "synthetic packet loss (prob=" + lossProbability
                            + ") on read for actor " + actorId);
        }
        final List<AuditEvent> upstream = delegate.read(actorId);
        return upstream == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(upstream));
    }

    public long getTotalAttempts() {
        return totalAttempts.get();
    }

    public long getInjectedFailures() {
        return injectedFailures.get();
    }

    private boolean shouldInjectLoss() {
        if (lossProbability <= 0.0) {
            return false;
        }
        if (lossProbability >= 1.0) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < lossProbability;
    }

    private static void sleep(final long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SimulatedPartitionException(
                    "interrupted while simulating latency");
        }
    }
}