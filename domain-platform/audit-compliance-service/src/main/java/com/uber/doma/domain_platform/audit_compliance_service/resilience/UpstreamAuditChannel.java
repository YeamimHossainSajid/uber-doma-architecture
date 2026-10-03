package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import java.util.List;

/**
 * Contract for the upstream audit log transport (Kafka topic, gRPC streaming,
 * append-only DB). Implemented by production wiring (out of scope here) and
 * by chaos-test stubs that simulate network partitions and packet loss.
 *
 * <p>Implementations of {@link #persist} are expected to throw a
 * {@link RuntimeException} (typically an IOException wrapped in a runtime
 * type, a serialization error) when the upstream is unreachable. The
 * {@link ResilientAuditSink} translates those into local buffering.</p>
 */
public interface UpstreamAuditChannel {

    /**
     * Persists a single audit event to the upstream transport.
     *
     * @throws RuntimeException if the transport is unreachable, the event
     *         cannot be serialized, or the upstream rejects the write.
     */
    void persist(AuditEvent event);

    /**
     * Reads audit history for the given actor from the upstream transport.
     *
     * @throws RuntimeException if the transport is unreachable.
     */
    List<AuditEvent> read(String actorId);
}