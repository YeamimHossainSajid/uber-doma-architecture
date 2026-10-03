package com.uber.doma.domain_platform.audit_compliance_service.spatial;

/**
 * Type-safe wrapper around the H3 resolution integers used by the
 * audit-compliance-service. Only the resolutions called out in the
 * architectural context (Resolution 8 and 9) are exposed as named
 * constants; callers requiring other resolutions can pass an integer
 * to {@link H3SpatialQueryService#geoToH3Address(double, double, int)}.
 *
 * <p>H3 resolution trade-offs (approximate):</p>
 * <ul>
 *   <li><b>Resolution 8</b> — ~0.74 km² hex area, ~960 m edge length.
 *       Useful for city-block level clustering of audit events.</li>
 *   <li><b>Resolution 9</b> — ~0.10 km² hex area, ~460 m edge length.
 *       Useful for building / pickup-spot level clustering.</li>
 * </ul>
 *
 * <p>Reference: https://h3geo.org/docs/core-library/restable/</p>
 */
public enum H3Resolution {

    /** City-block scale (~0.74 km²). Default for coarse geo-clustering. */
    RESOLUTION_8(8),

    /** Building scale (~0.10 km²). Default for fine-grained queries. */
    RESOLUTION_9(9);

    private final int value;

    H3Resolution(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Returns the H3 resolution enum for a given integer value, or
     * {@code null} if the value does not map to a known named constant.
     */
    public static H3Resolution fromValue(final int value) {
        for (final H3Resolution r : values()) {
            if (r.value == value) {
                return r;
            }
        }
        return null;
    }
}