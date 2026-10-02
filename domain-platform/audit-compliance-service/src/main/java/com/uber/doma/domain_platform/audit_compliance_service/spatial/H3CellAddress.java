package com.uber.doma.domain_platform.audit_compliance_service.spatial;

import java.util.Objects;

/**
 * Strongly-typed wrapper around a single H3 cell identifier. The H3
 * library internally stores the cell as a {@code long}, but the audit
 * layer persists the hexadecimal representation ({@link #address}) so that
 * the index is portable across systems and storage backends.
 *
 * <p>An H3 cell address is a 15-character lowercase hexadecimal string
 * (e.g. {@code "8928308280fffff"}). The address is independent of
 * resolution: the resolution is encoded in the address itself and is
 * recovered via {@link #resolution()}.</p>
 */
public final class H3CellAddress {

    private final String address;
    private final long longValue;
    private final int resolution;

    public H3CellAddress(final String address, final long longValue, final int resolution) {
        this.address = Objects.requireNonNull(address, "address");
        this.longValue = longValue;
        this.resolution = resolution;
    }

    /** Returns the canonical 15-character hex string representation. */
    public String address() {
        return address;
    }

    /** Returns the raw long index (useful for in-memory joins). */
    public long longValue() {
        return longValue;
    }

    /** Returns the H3 resolution of this cell (0–15). */
    public int resolution() {
        return resolution;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof H3CellAddress)) {
            return false;
        }
        final H3CellAddress that = (H3CellAddress) other;
        return longValue == that.longValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(longValue);
    }

    @Override
    public String toString() {
        return "H3CellAddress{" + address + " res=" + resolution + '}';
    }
}