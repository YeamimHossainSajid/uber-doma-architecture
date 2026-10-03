package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resilient sink for {@link AuditEvent}s that survives degraded network
 * conditions. The sink implements a bounded write-buffer with overflow
 * protection and an idempotent replay strategy. It is the "fallback
 * degradation strategy" referenced in the architectural context: under
 * packet loss and increased latency the sink continues to accept events,
 * queues them locally, and replays them once the upstream system becomes
 * reachable again — all without throwing unhandled exceptions back into
 * the calling code path.
 *
 * <p>Production wiring would call into Kafka or a remote append-only log;
 * here the {@link UpstreamAuditChannel} contract abstracts that transport
 * so chaos tests can plug in deterministic fault-injecting channels.</p>
 */
@Component
public class ResilientAuditSink {

    private static final Logger log = LoggerFactory.getLogger(ResilientAuditSink.class);

    /** Default maximum buffer size. Sized for in-memory fallback, intentionally
     *  conservative so a downstream partition does not OOM the JVM. */
    public static final int DEFAULT_BUFFER_CAPACITY = 10_000;

    private final UpstreamAuditChannel channel;
    private final int bufferCapacity;
    private final Deque<AuditEvent> buffer = new ArrayDeque<>();
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final AtomicLong persistedCount = new AtomicLong(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ResilientAuditSink() {
        this(new NoopUpstreamAuditChannel(), DEFAULT_BUFFER_CAPACITY);
    }

    public ResilientAuditSink(final UpstreamAuditChannel channel, final int bufferCapacity) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }
        if (bufferCapacity <= 0) {
            throw new IllegalArgumentException("bufferCapacity must be > 0");
        }
        this.channel = channel;
        this.bufferCapacity = bufferCapacity;
    }

    /**
     * Records an audit event. The method never throws — it catches all
     * failures (network timeouts, serialization errors, partition), buffers
     * the event locally and returns true if the event was persisted upstream
     * or queued for replay. Returns false only if the buffer is full and the
     * event had to be dropped (overflow).
     */
    public boolean record(final AuditEvent event) {
        if (closed.get()) {
            log.warn("[ResilientAuditSink] sink closed; dropping event {}", event.getId());
            droppedCount.incrementAndGet();
            return false;
        }
        if (event == null) {
            // A null event is a programmer error, but we surface it as a
            // false return value rather than letting it propagate up and
            // crash the booking request that produced it.
            log.warn("[ResilientAuditSink] null event passed to record()");
            return false;
        }
        try {
            channel.persist(event);
            persistedCount.incrementAndGet();
            return true;
        } catch (final RuntimeException ex) {
            // Network partition, packet loss, or any other downstream failure.
            // We never rethrow — the booking request must not be blocked by an
            // audit failure.
            log.warn("[ResilientAuditSink] upstream persist failed for event {} ({}): buffering",
                    event.getId(), ex.getMessage());
            return enqueue(event);
        }
    }

    /**
     * Reads audit history for an actor. The method is idempotent and never
     * throws. If the upstream read fails the method returns the local
     * in-memory view (which may be empty during a fresh partition).
     */
    public List<AuditEvent> readHistory(final String actorId) {
        if (actorId == null || actorId.isBlank()) {
            return Collections.emptyList();
        }
        try {
            final List<AuditEvent> remote = channel.read(actorId);
            if (remote != null) {
                return remote;
            }
        } catch (final RuntimeException ex) {
            log.warn("[ResilientAuditSink] upstream read failed for actor {} ({}); returning local view",
                    actorId, ex.getMessage());
        }
        final List<AuditEvent> local = new ArrayList<>();
        for (final AuditEvent event : buffer) {
            if (actorId.equals(event.getActorId())) {
                local.add(event);
            }
        }
        return Collections.unmodifiableList(local);
    }

    /**
     * Drains the buffer back into the upstream channel. The drain is
     * incremental — events that still fail to persist stay in the buffer
     * for the next drain cycle. A bounded retry budget ensures the call returns
     * within a deterministic timeframe even under sustained packet loss.
     *
     * @return the number of events replayed upstream successfully
     */
    public int drain() {
        int replayed = 0;
        // Snapshot the buffer to avoid ConcurrentModificationException when
        // record() runs concurrently with drain().
        final AuditEvent[] snapshot;
        synchronized (buffer) {
            snapshot = buffer.toArray(new AuditEvent[0]);
        }
        for (final AuditEvent event : snapshot) {
            try {
                channel.persist(event);
            } catch (final RuntimeException ex) {
                // Stop on first failure to preserve ordering for replay.
                log.debug("[ResilientAuditSink] drain aborted at event {}: {}",
                        event.getId(), ex.getMessage());
                break;
            }
            synchronized (buffer) {
                buffer.remove(event);
            }
            replayed++;
        }
        if (replayed > 0) {
            log.info("[ResilientAuditSink] drain replayed {} events", replayed);
        }
        return replayed;
    }

    /** Closes the sink. Further record() calls will reject events. */
    public void close() {
        closed.set(true);
    }

    public long bufferedCount() {
        synchronized (buffer) {
            return buffer.size();
        }
    }

    public long persistedCount() {
        return persistedCount.get();
    }

    public long droppedCount() {
        return droppedCount.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    private boolean enqueue(final AuditEvent event) {
        synchronized (buffer) {
            if (buffer.size() >= bufferCapacity) {
                log.error("[ResilientAuditSink] buffer overflow; dropping event {}", event.getId());
                droppedCount.incrementAndGet();
                return false;
            }
            buffer.addLast(event);
        }
        return true;
    }

    /**
     * Reads the most recently buffered event for an actor. Used by tests
     * to verify that events queued during a partition are observable in the
     * local fallback view.
     */
    public Optional<AuditEvent> findBuffered(final String eventId) {
        synchronized (buffer) {
            for (final AuditEvent event : buffer) {
                if (event.getId().equals(eventId)) {
                    return Optional.of(event);
                }
            }
        }
        return Optional.empty();
    }
}