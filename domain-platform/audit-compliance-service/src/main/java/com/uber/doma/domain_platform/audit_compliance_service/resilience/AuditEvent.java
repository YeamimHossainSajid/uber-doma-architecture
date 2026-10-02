package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a single audit log record produced by an
 * upstream booking/identity/payment system. The audit-compliance-service is
 * the canonical store of regulatory audit records; the {@link AuditEvent}
 * value object is the unit of work that flows through the resilience layer
 * (buffer, fallback, replay).
 *
 * <p>This object is intentionally simple: no framework annotations, no
 * serialization concerns. Serialization is handled by the storage layer
 * (out of scope for the chaos test) and JSON contracts are defined in the
 * shared {@code proto-contracts} module.</p>
 */
public final class AuditEvent {

    private final String id;
    private final String actorId;
    private final String action;
    private final Instant occurredAt;
    private final Map<String, String> attributes;

    public AuditEvent(final String id,
                      final String actorId,
                      final String action,
                      final Instant occurredAt,
                      final Map<String, String> attributes) {
        this.id = Objects.requireNonNull(id, "id");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.action = Objects.requireNonNull(action, "action");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Convenience factory that builds a deterministic event id from a
     * random UUID. Useful in tests where a stable identifier is desirable.
     */
    public static AuditEvent of(final String actorId,
                                 final String action,
                                 final Map<String, String> attributes) {
        return new AuditEvent(UUID.randomUUID().toString(),
                actorId, action, Instant.now(), attributes);
    }

    public String getId() {
        return id;
    }

    public String getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuditEvent)) {
            return false;
        }
        final AuditEvent that = (AuditEvent) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "AuditEvent{id=" + id
                + ", actorId=" + actorId
                + ", action=" + action
                + ", occurredAt=" + occurredAt
                + '}';
    }
}