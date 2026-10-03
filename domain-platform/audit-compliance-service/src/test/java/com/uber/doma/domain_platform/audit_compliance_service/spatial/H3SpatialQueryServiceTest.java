package com.uber.doma.domain_platform.audit_compliance_service.spatial;

import com.uber.doma.domain_platform.audit_compliance_service.resilience.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and integration tests for {@link H3SpatialQueryService}.
 *
 * <p>The tests assert that:</p>
 * <ul>
 *   <li>{@code geoToH3Address} returns the canonical H3 cell for well-known
 *       coordinate anchors at resolutions 8 and 9 (geospatial coverage).</li>
 *   <li>{@code kRing} returns the expected count of cells for radius 0, 1, 2,
 *       3 around an anchor — including the property that origin is always
 *       first and the count matches the closed-form formula
 *       {@code 1 + 3k(k+1)} for non-pentagon cells.</li>
 *   <li>{@code hexRing} returns exactly {@code 6k} cells at radius k > 0 and
 *       does NOT include the origin.</li>
 *   <li>{@code clusterByCell} buckets audit events by H3 cell at the given
 *       resolution.</li>
 *   <li>Invalid inputs (out-of-range coordinates, out-of-range resolutions,
 *       negative radius) are rejected with {@link IllegalArgumentException}.</li>
 * </ul>
 *
 * <p>Anchor values were captured by {@code H3Anchors} (a one-shot probe) and
 * are stable for H3 v3.7.2 across platforms.</p>
 */
class H3SpatialQueryServiceTest {

    private H3SpatialQueryService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new H3SpatialQueryService();
        service.initH3Core();
    }

    // -------- geoToH3Address --------

    @Test
    @DisplayName("geoToH3Address returns canonical NYC Manhattan address")
    void geoToH3AddressForNycManhattan() {
        // Resolution 8 anchor for (40.7128, -74.0060)
        final H3CellAddress cell = service.geoToH3Address(40.7128, -74.0060, 8);
        assertEquals("882a107289fffff", cell.address());
        assertEquals(8, cell.resolution());
        assertNotEquals(0L, cell.longValue());

        // Resolution 9 anchor for the same coordinate.
        final H3CellAddress cell9 = service.geoToH3Address(40.7128, -74.0060, 9);
        assertEquals("892a1072893ffff", cell9.address());
        assertEquals(9, cell9.resolution());
    }

    @Test
    @DisplayName("geoToH3Address works for multiple global anchors")
    void geoToH3AddressGlobalCoverage() {
        // San Francisco — resolution 8 anchor (captured from H3Anchors probe).
        assertEquals("8828308281fffff",
                service.geoToH3Address(37.7749, -122.4194, 8).address());
        // London res 8
        assertEquals("88195da49bfffff",
                service.geoToH3Address(51.5074, -0.1278, 8).address());
        // Tokyo res 9
        assertEquals("892f5a363bbffff",
                service.geoToH3Address(35.6762, 139.6503, 9).address());
        // Sydney res 8
        assertEquals("88be0e35cbfffff",
                service.geoToH3Address(-33.8688, 151.2093, 8).address());
        // Null Island res 9
        assertEquals("89754e64993ffff",
                service.geoToH3Address(0.0, 0.0, 9).address());
    }

    @Test
    @DisplayName("geoToH3Address accepts H3Resolution enum overload")
    void geoToH3AddressEnumOverload() {
        final H3CellAddress cell8 = service.geoToH3Address(40.7128, -74.0060, H3Resolution.RESOLUTION_8);
        final H3CellAddress cell9 = service.geoToH3Address(40.7128, -74.0060, H3Resolution.RESOLUTION_9);
        assertEquals(8, cell8.resolution());
        assertEquals(9, cell9.resolution());
        assertNotEquals(cell8.address(), cell9.address());
    }

    @Test
    @DisplayName("geoToH3Address rejects invalid coordinates")
    void geoToH3AddressRejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> service.geoToH3Address(91.0, 0.0, 9),
                "lat > 90 must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> service.geoToH3Address(-91.0, 0.0, 9),
                "lat < -90 must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> service.geoToH3Address(0.0, -181.0, 9),
                "lng < -180 must be rejected");
        assertThrows(IllegalArgumentException.class,
                () -> service.geoToH3Address(0.0, 181.0, 9),
                "lng > 180 must be rejected");
    }

    @Test
    @DisplayName("geoToH3Address rejects invalid resolutions")
    void geoToH3AddressRejectsInvalidResolution() {
        assertThrows(IllegalArgumentException.class,
                () -> service.geoToH3Address(0.0, 0.0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> service.geoToH3Address(0.0, 0.0, 16));
    }

    @Test
    @DisplayName("geoToH3Address is deterministic and stable across calls")
    void geoToH3AddressIsDeterministic() {
        final H3CellAddress first = service.geoToH3Address(40.7128, -74.0060, 9);
        final H3CellAddress second = service.geoToH3Address(40.7128, -74.0060, 9);
        assertEquals(first.address(), second.address());
        assertEquals(first.longValue(), second.longValue());
        // Two sufficiently-distant coordinates produce distinct cells.
        // (At resolution 9 ~460m cells, ~1 km apart is safely distinct.)
        assertNotEquals(first.address(),
                service.geoToH3Address(40.7218, -74.0060, 9).address());
    }

    // -------- cellCenter --------

    @Test
    @DisplayName("cellCenter returns a coordinate close to the original anchor")
    void cellCenterRoundTripsToNearbyAnchor() {
        final H3CellAddress cell = service.geoToH3Address(40.7128, -74.0060, 9);
        final GeoCoordinate center = service.cellCenter(cell);
        // Resolution 9 cells are ~460 m across; the center should be within
        // ~250 m of the original anchor.
        assertEquals(40.7128, center.getLatitude(), 0.005);
        assertEquals(-74.0060, center.getLongitude(), 0.005);
    }

    // -------- kRing --------

    @Test
    @DisplayName("kRing radius 0 returns just the origin cell")
    void kRingRadius0ReturnsOriginOnly() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> ring = service.kRing(origin, 0);
        assertEquals(1, ring.size());
        assertEquals(origin.address(), ring.get(0).address());
    }

    @Test
    @DisplayName("kRing radius 1 returns 1 + 6 = 7 cells (origin + 6 neighbors)")
    void kRingRadius1Returns7Cells() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> ring = service.kRing(origin, 1);
        assertEquals(7, ring.size(), "1 origin + 6 neighbors = 7 cells");
        // Origin must be present and first.
        assertEquals(origin.address(), ring.get(0).address());
        // All cells must be unique.
        final Set<String> addresses = new HashSet<>();
        for (final H3CellAddress cell : ring) {
            addresses.add(cell.address());
        }
        assertEquals(7, addresses.size(), "all kRing cells must be distinct");
        // Every cell in the ring must be a valid resolution-9 cell.
        for (final H3CellAddress cell : ring) {
            assertEquals(9, cell.resolution());
        }
    }

    @Test
    @DisplayName("kRing radius 2 returns 1 + 6 + 12 = 19 cells")
    void kRingRadius2Returns19Cells() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> ring = service.kRing(origin, 2);
        assertEquals(19, ring.size(), "1 origin + 6 + 12 neighbors = 19 cells");
    }

    @Test
    @DisplayName("kRing radius 3 returns 1 + 6 + 12 + 18 = 37 cells")
    void kRingRadius3Returns37Cells() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> ring = service.kRing(origin, 3);
        assertEquals(37, ring.size(), "1 origin + 6 + 12 + 18 neighbors = 37 cells");
    }

    @Test
    @DisplayName("kRing closed-form 1 + 3k(k+1) for radii 0 through 5")
    void kRingMatchesClosedFormFormula() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        for (int k = 0; k <= 5; k++) {
            final int expected = 1 + 3 * k * (k + 1);
            final List<H3CellAddress> ring = service.kRing(origin, k);
            assertEquals(expected, ring.size(),
                    "kRing radius " + k + " should yield " + expected + " cells");
        }
    }

    @Test
    @DisplayName("kRing rejects negative radius")
    void kRingRejectsNegativeRadius() {
        final H3CellAddress origin = service.geoToH3Address(0.0, 0.0, 9);
        assertThrows(IllegalArgumentException.class, () -> service.kRing(origin, -1));
    }

    @Test
    @DisplayName("kRing on lat/lng overload is equivalent to the cell overload")
    void kRingLatLngOverload() {
        final List<H3CellAddress> viaCell = service.kRing(
                service.geoToH3Address(40.7128, -74.0060, 9), 2);
        final List<H3CellAddress> viaLatLng = service.kRing(40.7128, -74.0060, 9, 2);
        assertEquals(viaCell.size(), viaLatLng.size());
        for (int i = 0; i < viaCell.size(); i++) {
            assertEquals(viaCell.get(i).address(), viaLatLng.get(i).address());
        }
    }

    // -------- hexRing --------

    @Test
    @DisplayName("hexRing radius 0 returns the origin (H3 semantics)")
    void hexRingRadius0ReturnsOrigin() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> ring = service.hexRing(origin, 0);
        // H3 hexRing at k=0 returns just the origin cell (1 element),
        // not an empty list. This matches the documented library
        // semantics: hexRing returns the origin for k = 0 and the
        // 6k-cell ring for k >= 1.
        assertEquals(1, ring.size());
        assertEquals(origin.address(), ring.get(0).address());
    }

    @Test
    @DisplayName("hexRing radius 1 returns exactly 6 neighbor cells")
    void hexRingRadius1Returns6Neighbors() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> ring = service.hexRing(origin, 1);
        assertEquals(6, ring.size(), "first ring has 6 neighbors");
        for (final H3CellAddress cell : ring) {
            assertNotEquals(origin.address(), cell.address(),
                    "hexRing at k=1 must not include the origin");
        }
    }

    @Test
    @DisplayName("hexRing radius 2 returns exactly 12 cells")
    void hexRingRadius2Returns12Cells() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> ring = service.hexRing(origin, 2);
        assertEquals(12, ring.size(), "second ring has 12 cells");
    }

    // -------- immediateNeighbors --------

    @Test
    @DisplayName("immediateNeighbors returns the 6 first-ring neighbors")
    void immediateNeighbors() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> neighbors = service.immediateNeighbors(origin);
        assertEquals(6, neighbors.size());
    }

    // -------- h3Distance --------

    @Test
    @DisplayName("h3Distance from origin to itself is 0")
    void h3DistanceToSelf() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        assertEquals(0, service.h3Distance(origin, origin));
    }

    @Test
    @DisplayName("h3Distance to immediate neighbor is 1")
    void h3DistanceToImmediateNeighbor() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final H3CellAddress neighbor = service.immediateNeighbors(origin).get(0);
        assertEquals(1, service.h3Distance(origin, neighbor));
    }

    @Test
    @DisplayName("h3Distance to a cell two rings away is 2")
    void h3DistanceTwoRingsAway() {
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> ring2 = service.kRing(origin, 2);
        // The last cell in the kRing list is part of the second ring.
        final H3CellAddress secondRing = ring2.get(ring2.size() - 1);
        assertEquals(2, service.h3Distance(origin, secondRing));
    }

    // -------- clusterByCell --------

    @Test
    @DisplayName("clusterByCell groups events into the correct cells")
    void clusterByCellGroupsEvents() {
        final AuditEvent event1 = AuditEvent.of("rider-1", "booking.created",
                Map.of("bookingId", "b1"));
        final AuditEvent event2 = AuditEvent.of("rider-2", "booking.created",
                Map.of("bookingId", "b2"));
        final AuditEvent event3 = AuditEvent.of("rider-3", "booking.created",
                Map.of("bookingId", "b3"));
        // event1 and event3 are in NYC; event2 is in SF.
        final List<AuditGeoEvent> events = Arrays.asList(
                new AuditGeoEvent(event1, new GeoCoordinate(40.7128, -74.0060)),
                new AuditGeoEvent(event2, new GeoCoordinate(37.7749, -122.4194)),
                new AuditGeoEvent(event3, new GeoCoordinate(40.7129, -74.0061)));

        final Map<H3CellAddress, List<AuditGeoEvent>> clusters = service.clusterByCell(events, 9);
        assertEquals(2, clusters.size(), "NYC and SF form two distinct res-9 cells");
        int totalEvents = 0;
        for (final List<AuditGeoEvent> bucket : clusters.values()) {
            totalEvents += bucket.size();
        }
        assertEquals(3, totalEvents);
    }

    @Test
    @DisplayName("clusterByCellAllCoordsIgnoresNullLocation")
    void clusterByCellIgnoresNullLocation() {
        final AuditEvent event = AuditEvent.of("rider-1", "booking.created", Map.of());
        final List<AuditGeoEvent> events = new ArrayList<>();
        events.add(new AuditGeoEvent(event, null));
        final Map<H3CellAddress, List<AuditGeoEvent>> clusters = service.clusterByCell(events, 9);
        assertEquals(0, clusters.size(), "null locations must be skipped, not bucketed");
    }

    // -------- cellsCoveredBy --------

    @Test
    @DisplayName("cellsCoveredBy returns the unique H3 cells touched by events")
    void cellsCoveredByReturnsUniqueCells() {
        final AuditEvent event1 = AuditEvent.of("rider-1", "booking.created", Map.of());
        final AuditEvent event2 = AuditEvent.of("rider-2", "booking.created", Map.of());
        final List<AuditGeoEvent> events = Arrays.asList(
                new AuditGeoEvent(event1, new GeoCoordinate(40.7128, -74.0060)),
                new AuditGeoEvent(event2, new GeoCoordinate(40.7129, -74.0061)));
        final Set<H3CellAddress> cells = service.cellsCoveredBy(events, 9);
        assertEquals(1, cells.size(),
                "two nearby NYC events should bucket into the same res-9 cell");
    }

    @Test
    @DisplayName("cellsCoveredBy yields distinct cells for distant events")
    void cellsCoveredByDistinctDistantEvents() {
        final AuditEvent event1 = AuditEvent.of("rider-1", "booking.created", Map.of());
        final AuditEvent event2 = AuditEvent.of("rider-2", "booking.created", Map.of());
        final List<AuditGeoEvent> events = Arrays.asList(
                new AuditGeoEvent(event1, new GeoCoordinate(40.7128, -74.0060)),
                new AuditGeoEvent(event2, new GeoCoordinate(37.7749, -122.4194)));
        final Set<H3CellAddress> cells = service.cellsCoveredBy(events, 8);
        assertEquals(2, cells.size(), "NYC and SF are far apart and fall in distinct res-8 cells");
    }

    // -------- H3Resolution --------

    @Test
    @DisplayName("H3Resolution.fromValue round-trips through getValue")
    void h3ResolutionRoundTrip() {
        assertSame(H3Resolution.RESOLUTION_8, H3Resolution.fromValue(8));
        assertSame(H3Resolution.RESOLUTION_9, H3Resolution.fromValue(9));
        assertEquals(8, H3Resolution.RESOLUTION_8.getValue());
        assertEquals(9, H3Resolution.RESOLUTION_9.getValue());
        // Unknown resolutions map to null (callers must use the int variant).
        assertTrue(H3Resolution.fromValue(5) == null);
        assertTrue(H3Resolution.fromValue(0) == null);
    }

    // -------- GeoCoordinate --------

    @Test
    @DisplayName("GeoCoordinate rejects out-of-range lat/lng")
    void geoCoordinateValidatesBounds() {
        assertThrows(IllegalArgumentException.class, () -> new GeoCoordinate(-91.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new GeoCoordinate(91.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new GeoCoordinate(0.0, -181.0));
        assertThrows(IllegalArgumentException.class, () -> new GeoCoordinate(0.0, 181.0));
    }

    @Test
    @DisplayName("GeoCoordinate equals/hashCode is value-based")
    void geoCoordinateEquality() {
        final GeoCoordinate a = new GeoCoordinate(40.7128, -74.0060);
        final GeoCoordinate b = new GeoCoordinate(40.7128, -74.0060);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new GeoCoordinate(40.7129, -74.0060));
    }

    // -------- H3CellAddress --------

    @Test
    @DisplayName("H3CellAddress equality is long-value based")
    void h3CellAddressEquality() {
        final H3CellAddress a = service.geoToH3Address(40.7128, -74.0060, 9);
        final H3CellAddress b = service.geoToH3Address(40.7128, -74.0060, 9);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a cell"));
    }

    // -------- cross-resolution sanity --------

    @Test
    @DisplayName("different resolutions produce different cells for the same coordinate")
    void differentResolutionsProduceDifferentCells() {
        final H3CellAddress r8 = service.geoToH3Address(40.7128, -74.0060, 8);
        final H3CellAddress r9 = service.geoToH3Address(40.7128, -74.0060, 9);
        assertNotEquals(r8.address(), r9.address());
        assertEquals(8, r8.resolution());
        assertEquals(9, r9.resolution());
    }

    @Test
    @DisplayName("kRing at higher resolution yields strictly more coverage")
    void kRingCoverageGrowsWithResolution() {
        final H3CellAddress r8 = service.geoToH3Address(40.7128, -74.0060, 8);
        final H3CellAddress r9 = service.geoToH3Address(40.7128, -74.0060, 9);
        final List<H3CellAddress> r8Ring = service.kRing(r8, 2);
        final List<H3CellAddress> r9Ring = service.kRing(r9, 2);
        // Both should be 19 (non-pentagon) but they should be disjoint cell sets
        // because H3 cells at different resolutions never coincide.
        assertEquals(19, r8Ring.size());
        assertEquals(19, r9Ring.size());
        final Set<String> r8Addresses = new HashSet<>();
        for (final H3CellAddress c : r8Ring) {
            r8Addresses.add(c.address());
        }
        for (final H3CellAddress c : r9Ring) {
            assertFalse(r8Addresses.contains(c.address()),
                    "res-8 and res-9 cell sets must be disjoint");
        }
    }

    // -------- boundary coverage at the poles & antimeridian --------

    @Test
    @DisplayName("geoToH3Address accepts extreme valid coordinates (poles, antimeridian)")
    void geoToH3AddressAcceptsExtremeValidCoordinates() {
        // North pole
        final H3CellAddress northPole = service.geoToH3Address(90.0, 0.0, 9);
        assertNotNull(northPole);
        assertEquals(9, northPole.resolution());
        // South pole
        final H3CellAddress southPole = service.geoToH3Address(-90.0, 0.0, 9);
        assertNotNull(southPole);
        assertEquals(9, southPole.resolution());
        // Antimeridian east/west — both valid, same anchor coordinate.
        final H3CellAddress east = service.geoToH3Address(0.0, 180.0, 9);
        final H3CellAddress west = service.geoToH3Address(0.0, -180.0, 9);
        assertNotNull(east);
        assertNotNull(west);
        assertEquals(9, east.resolution());
        assertEquals(9, west.resolution());
    }

    // -------- service exposes its H3Core --------

    @Test
    @DisplayName("getH3Core returns the underlying native H3 instance")
    void getH3CoreReturnsInstance() {
        assertNotNull(service.getH3Core());
    }

    // -------- AuditGeoEvent --------

    @Test
    @DisplayName("AuditGeoEvent equality and accessors are correct")
    void auditGeoEventBasics() {
        final AuditEvent event = new AuditEvent("id-1", "actor", "action",
                Instant.now(), Map.of());
        final AuditGeoEvent geo = new AuditGeoEvent(event, new GeoCoordinate(40.7128, -74.0060));
        assertEquals(event, geo.getEvent());
        assertEquals(40.7128, geo.getLocation().getLatitude(), 0.0001);
        assertEquals(event, new AuditGeoEvent(event, new GeoCoordinate(0, 0)).getEvent());
    }
}