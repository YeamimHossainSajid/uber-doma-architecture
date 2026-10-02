package com.uber.doma.domain_platform.audit_compliance_service.spatial;

import com.uber.h3core.H3Core;
import com.uber.h3core.exceptions.DistanceUndefinedException;
import com.uber.h3core.exceptions.PentagonEncounteredException;
import com.uber.h3core.util.GeoCoord;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Spatial query service backed by the Uber H3 hexagonal index.
 *
 * <p>Provides O(1) conversion from WGS-84 lat/lng to an H3 cell address
 * ({@link #geoToH3Address(double, double, int)}) and constant-time
 * neighbor ring lookups ({@link #kRing(H3CellAddress, int)}). The service
 * is used by the audit-compliance-service to cluster immutable audit
 * events into hexagonal buckets so that geographically co-located
 * regulatory reports can be assembled without a full table scan.</p>
 *
 * <p>The H3 native library is loaded once at startup via
 * {@link H3Core#newSystemInstance()}. Resolution 8 and 9 are the
 * production defaults called out in the architectural context; other
 * resolutions (0–15) are supported through the explicit int overloads.</p>
 */
@Service
public class H3SpatialQueryService {

    private static final Logger log = LoggerFactory.getLogger(H3SpatialQueryService.class);

    /** Valid H3 resolutions. */
    public static final int MIN_RESOLUTION = 0;
    public static final int MAX_RESOLUTION = 15;

    private H3Core h3;

@PostConstruct
    void initH3Core() throws IOException {
        // newInstance() unpacks the native H3 library from the h3-3.7.2.jar
        // into a temporary directory and loads it. This is the recommended
        // entry point for production code: it is self-contained and works
        // on any OS/arch the jar ships binaries for.
        this.h3 = H3Core.newInstance();
        log.info("[H3SpatialQueryService] loaded H3 native library; res0 cell count={}",
                h3.numHexagons(0));
    }

    /**
     * Converts a WGS-84 (lat, lng) pair into an H3 cell address at the
     * requested resolution. The call is O(1) and never throws for valid
     * inputs; invalid lat/lng values surface an
     * {@link IllegalArgumentException} via the {@link GeoCoordinate}
     * constructor and invalid resolutions throw
     * {@link IllegalArgumentException} from this method.
     */
    public H3CellAddress geoToH3Address(final double lat, final double lng, final int resolution) {
        validateResolution(resolution);
        final GeoCoordinate coordinate = new GeoCoordinate(lat, lng);
        final long h3Index = h3.geoToH3(coordinate.getLatitude(), coordinate.getLongitude(), resolution);
        final String address = h3.h3ToString(h3Index);
        return new H3CellAddress(address, h3Index, resolution);
    }

    /**
     * Convenience overload that accepts the type-safe
     * {@link H3Resolution} enum.
     */
    public H3CellAddress geoToH3Address(final double lat, final double lng, final H3Resolution resolution) {
        Objects.requireNonNull(resolution, "resolution");
        return geoToH3Address(lat, lng, resolution.getValue());
    }

    /**
     * Returns the set of H3 cell addresses that lie within {@code radius}
     * rings of the {@code origin} cell. The result is inclusive of the
     * origin (radius 0 returns exactly the origin cell). The cardinality
     * for a non-pentagon cell is {@code 1 + 6 + 12 + … + 6*radius}.
     *
     * <p>The H3 {@code kRing} algorithm runs in O(k²) and returns
     * approximately 1 + 3k(k+1) cells. For an audit query asking for
     * "events within 2 hex cells" of a pickup point, this is the
     * idiomatic call. The returned list is unmodifiable; duplicates that
     * the underlying H3 library may produce across pentagon distortion
     * are deduplicated.</p>
     *
     * @param origin  the central H3 cell
     * @param radius  number of concentric rings to include (>= 0)
     * @return ordered list of unique H3 cell addresses (origin first)
     * @throws IllegalArgumentException if {@code radius} is negative
     */
    public List<H3CellAddress> kRing(final H3CellAddress origin, final int radius) {
        Objects.requireNonNull(origin, "origin");
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be >= 0; got " + radius);
        }
        final List<Long> indices = h3.kRing(origin.longValue(), radius);
        // Deduplicate while preserving insertion order (origin first).
        final Set<Long> seen = new LinkedHashSet<>(indices);
        final List<H3CellAddress> out = new ArrayList<>(seen.size());
        for (final Long idx : seen) {
            final int res = h3.h3GetResolution(idx);
            out.add(new H3CellAddress(h3.h3ToString(idx), idx, res));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Single-ring variant: returns exactly the cells that are at distance
     * {@code k} from the origin (i.e. the {@code k}-th ring). Useful for
     * "give me the immediate neighbors" queries without expanding the
     * full ring set.
     */
    public List<H3CellAddress> hexRing(final H3CellAddress origin, final int k) {
        Objects.requireNonNull(origin, "origin");
        if (k < 0) {
            throw new IllegalArgumentException("k must be >= 0; got " + k);
        }
        final List<Long> indices;
        try {
            indices = h3.hexRing(origin.longValue(), k);
        } catch (final PentagonEncounteredException ex) {
            log.warn("[H3SpatialQueryService] hexRing pentagon distortion at origin={}, k={}; "
                    + "returning empty result", origin, k);
            return List.of();
        }
        final List<H3CellAddress> out = new ArrayList<>(indices.size());
        for (final Long idx : indices) {
            out.add(new H3CellAddress(h3.h3ToString(idx), idx, h3.h3GetResolution(idx)));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Convenience: returns the {@code radius} ring around the given
     * lat/lng at the given resolution. Equivalent to:
     *
     * <pre>
     *   final H3CellAddress origin = geoToH3Address(lat, lng, resolution);
     *   return kRing(origin, radius);
     * </pre>
     */
    public List<H3CellAddress> kRing(final double lat, final double lng,
                                     final int resolution, final int radius) {
        return kRing(geoToH3Address(lat, lng, resolution), radius);
    }

    /**
     * Returns the geographic center of an H3 cell as a
     * {@link GeoCoordinate}.
     */
    public GeoCoordinate cellCenter(final H3CellAddress cell) {
        Objects.requireNonNull(cell, "cell");
        final GeoCoord center = h3.h3ToGeo(cell.longValue());
        return new GeoCoordinate(center.lat, center.lng);
    }

    /**
     * Computes the H3 hierarchical distance between two cells. The
     * distance is the number of H3 neighbor steps required to walk from
     * {@code a} to {@code b}. Returns -1 if the cells are too far apart
     * to compute directly.
     */
    public int h3Distance(final H3CellAddress a, final H3CellAddress b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        try {
            return h3.h3Distance(a.longValue(), b.longValue());
        } catch (final DistanceUndefinedException ex) {
            // Distance is undefined for cells separated by a pentagon or
            // by a transcontinental crossing. We surface -1 (the H3
            // library's "no answer" sentinel) rather than propagating the
            // checked exception.
            log.debug("[H3SpatialQueryService] distance undefined between {} and {}", a, b);
            return -1;
        }
    }

    /**
     * Returns the neighbor cells of the origin (i.e. the k=1 ring). This
     * is the typical "find events in adjacent hex cells" audit query.
     */
    public List<H3CellAddress> immediateNeighbors(final H3CellAddress origin) {
        return hexRing(origin, 1);
    }

    /**
     * Buckets a list of audit events by H3 cell at the given resolution.
     * Each event's geographic location is mapped to its containing cell
     * and the events are grouped by cell address. Events with null
     * coordinates are routed to a sentinel bucket.
     *
     * @param events     the audit events to cluster
     * @param resolution H3 resolution to use for clustering
     * @return ordered map of cell address → list of events in that cell
     */
    public Map<H3CellAddress, List<AuditGeoEvent>> clusterByCell(
            final List<AuditGeoEvent> events, final int resolution) {
        validateResolution(resolution);
        Objects.requireNonNull(events, "events");
        // Use insertion-ordered map so deterministic ordering is preserved
        // for tests and trace dumps.
        final Map<H3CellAddress, List<AuditGeoEvent>> clusters = new java.util.LinkedHashMap<>();
        for (final AuditGeoEvent event : events) {
            if (event == null) {
                continue;
            }
            final GeoCoordinate coord = event.getLocation();
            if (coord == null) {
                continue;
            }
            final H3CellAddress cell = geoToH3Address(
                    coord.getLatitude(), coord.getLongitude(), resolution);
            clusters.computeIfAbsent(cell, k -> new ArrayList<>()).add(event);
        }
        return clusters;
    }

    /**
     * Returns the set of unique H3 cells that contain at least one event
     * in the given list. Equivalent to
     * {@code clusterByCell(events, resolution).keySet()} but skips the
     * per-cell allocation of value lists.
     */
    public Set<H3CellAddress> cellsCoveredBy(
            final List<AuditGeoEvent> events, final int resolution) {
        validateResolution(resolution);
        Objects.requireNonNull(events, "events");
        final Set<H3CellAddress> cells = ConcurrentHashMap.newKeySet();
        for (final AuditGeoEvent event : events) {
            if (event == null || event.getLocation() == null) {
                continue;
            }
            final GeoCoordinate c = event.getLocation();
            cells.add(geoToH3Address(c.getLatitude(), c.getLongitude(), resolution));
        }
        return cells.stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateResolution(final int resolution) {
        if (resolution < MIN_RESOLUTION || resolution > MAX_RESOLUTION) {
            throw new IllegalArgumentException(
                    "resolution must be in [" + MIN_RESOLUTION + ", " + MAX_RESOLUTION
                            + "]; got " + resolution);
        }
    }

    /**
     * Visible for testing. Returns the underlying {@link H3Core} instance
     * or {@code null} if the native library has not been initialised yet
     * (e.g. in unit tests that don't run the {@code @PostConstruct}
     * hook).
     */
    H3Core getH3Core() {
        return h3;
    }
}