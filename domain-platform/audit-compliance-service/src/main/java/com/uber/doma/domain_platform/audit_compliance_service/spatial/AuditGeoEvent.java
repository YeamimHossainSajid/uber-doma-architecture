package com.uber.doma.domain_platform.audit_compliance_service.spatial;

import com.uber.doma.domain_platform.audit_compliance_service.resilience.AuditEvent;

import java.util.Objects;

/**
 * Adapter that augments an immutable {@link AuditEvent} with a
 * geographic location. Audit events that carry lat/lng metadata (booking
 * pickup, GPS ping, location-based permission grant, …) are wrapped in
 * this class before being fed to {@link H3SpatialQueryService#clusterByCell}.
 *
 * <p>The wrapper does not copy the underlying event; it stores a
 * reference, so memory pressure is O(1) per geo-tagged event.</p>
 */
public final class AuditGeoEvent {

    private final AuditEvent event;
    private final GeoCoordinate location;

    public AuditGeoEvent(final AuditEvent event, final GeoCoordinate location) {
        this.event = Objects.requireNonNull(event, "event");
        this.location = location;
    }

    public AuditEvent getEvent() {
        return event;
    }

    public GeoCoordinate getLocation() {
        return location;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuditGeoEvent)) {
            return false;
        }
        final AuditGeoEvent that = (AuditGeoEvent) other;
        return event.equals(that.event);
    }

    @Override
    public int hashCode() {
        return event.hashCode();
    }

    @Override
    public String toString() {
        return "AuditGeoEvent{id=" + event.getId() + ", location=" + location + '}';
    }
}