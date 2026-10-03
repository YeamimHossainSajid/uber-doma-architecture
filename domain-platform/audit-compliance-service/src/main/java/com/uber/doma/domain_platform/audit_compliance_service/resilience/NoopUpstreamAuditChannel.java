package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import java.util.Collections;
import java.util.List;

/**
 * Default no-op implementation of {@link UpstreamAuditChannel}. Used when
 * the resilience layer is wired without an upstream transport (local
 * development, integration tests, sanity checks).
 */
public class NoopUpstreamAuditChannel implements UpstreamAuditChannel {

    @Override
    public void persist(final AuditEvent event) {
        // No-op. Production wiring replaces this bean with a Kafka-backed
        // implementation.
    }

    @Override
    public List<AuditEvent> read(final String actorId) {
        return Collections.emptyList();
    }
}