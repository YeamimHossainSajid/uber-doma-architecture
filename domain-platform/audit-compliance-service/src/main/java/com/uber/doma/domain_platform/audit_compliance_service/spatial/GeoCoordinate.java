package com.uber.doma.domain_platform.audit_compliance_service.spatial;

import java.util.Objects;

/**
 * Immutable WGS-84 lat/lng pair used as input to H3 conversion and as the
 * representation of an audit event's geographic origin (pickup, dropoff,
 * GPS ping, …).
 *
 * <p>Validation bounds match the WGS-84 spec:
 * <ul>
 *   <li>latitude ∈ [-90.0, 90.0]</li>
 *   <li>longitude ∈ [-180.0, 180.0]</li>
 * </ul>
 */
public final class GeoCoordinate {

    private final double latitude;
    private final double longitude;

    public GeoCoordinate(final double latitude, final double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude must be in [-90, 90]; got " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude must be in [-180, 180]; got " + longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GeoCoordinate)) {
            return false;
        }
        final GeoCoordinate that = (GeoCoordinate) other;
        return Double.compare(latitude, that.latitude) == 0
                && Double.compare(longitude, that.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return "GeoCoordinate{lat=" + latitude + ", lng=" + longitude + '}';
    }
}