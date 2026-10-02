package com.uber.doma.domain_platform.audit_compliance_service.spatial;

import com.uber.doma.domain_platform.audit_compliance_service.resilience.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests that exercise {@link H3SpatialQueryService} against
 * realistic audit-event distribution patterns. The tests verify that
 * {@code kRing}-driven "find events in nearby cells" queries correctly
 * compose with the spatial clustering helpers to support a regulatory
 * audit query path: "list every audit event within 2 cells of this
 * pickup location at resolution 9."
 */
class H3SpatialAuditQueryIntegrationTest {

    private H3SpatialQueryService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new H3SpatialQueryService();
        service.initH3Core();
    }

    @Test
    @DisplayName("regulatory query: events within 2 rings of a pickup at res 9")
    void regulatoryQueryFindsEventsWithinTwoRings() {
        // 25 pickup events scattered within ~1.5 km of Times Square.
        final List<AuditGeoEvent> pickups = generateAuditEventsAround(
                new GeoCoordinate(40.7580, -73.9855), 0.015, 25);
        final Map<H3CellAddress, List<AuditGeoEvent>> clusters = service.clusterByCell(pickups, 9);
        assertFalse(clusters.isEmpty(), "pickups should bucket into at least one res-9 cell");

        // Compose the regulatory query: union of kRing(2) over each cell.
        final Set<H3CellAddress> cellsWithinTwoRings = new LinkedHashSet<>();
        for (final H3CellAddress cell : clusters.keySet()) {
            cellsWithinTwoRings.addAll(service.kRing(cell, 2));
        }
        // Each unique cell within 2 rings contributes its events.
        int eventsFound = 0;
        for (final H3CellAddress cell : cellsWithinTwoRings) {
            final List<AuditGeoEvent> bucket = clusters.get(cell);
            if (bucket != null) {
                eventsFound += bucket.size();
            }
        }
        assertEquals(pickups.size(), eventsFound,
                "every pickup event must be reachable via a 2-ring kRing of its own cell");
    }

    @Test
    @DisplayName("resolution 9 produces at least as many cells as resolution 8")
    void resolutionChoiceAffectsGranularity() {
        // Three pickup events that are spread apart in Manhattan.
        final List<AuditGeoEvent> events = Arrays.asList(
                geoEvent("nyc-1", 40.7580, -73.9855),
                geoEvent("nyc-2", 40.7800, -73.9700),
                geoEvent("nyc-3", 40.7400, -74.0050));

        final Map<H3CellAddress, List<AuditGeoEvent>> res8 = service.clusterByCell(events, 8);
        final Map<H3CellAddress, List<AuditGeoEvent>> res9 = service.clusterByCell(events, 9);

        // Each event lands in its own cell at both resolutions.
        assertEquals(3, res8.size(), "3 spread-out events → 3 distinct res-8 cells");
        assertEquals(3, res9.size(), "3 spread-out events → 3 distinct res-9 cells");

        // Res 8 and res 9 cell sets are disjoint (different resolutions).
        final Set<String> res8Addresses = new HashSet<>();
        for (final H3CellAddress c : res8.keySet()) {
            res8Addresses.add(c.address());
        }
        for (final H3CellAddress c : res9.keySet()) {
            assertFalse(res8Addresses.contains(c.address()),
                    "res-8 and res-9 cells must be disjoint even when sharing a coordinate");
        }
    }

    @Test
    @DisplayName("kRing on res 8 origin covers all NYC events in the dataset")
    void kRingCoversAllEvents() {
        final H3CellAddress origin = service.geoToH3Address(40.7580, -73.9855, 8);
        final List<H3CellAddress> ring = service.kRing(origin, 3);

        // Generate 100 events within ~1.5 km of the origin.
        final List<AuditGeoEvent> events = generateAuditEventsAround(
                new GeoCoordinate(40.7580, -73.9855), 0.015, 100);
        final Map<H3CellAddress, List<AuditGeoEvent>> clusters = service.clusterByCell(events, 8);

        // The kRing at radius 3 (in res 8, ~3.6 km diameter) must cover
        // every cluster bucket.
        final Set<String> ringAddresses = new HashSet<>();
        for (final H3CellAddress c : ring) {
            ringAddresses.add(c.address());
        }
        for (final H3CellAddress cell : clusters.keySet()) {
            assertTrue(ringAddresses.contains(cell.address()),
                    "every cluster cell must lie within the 3-ring coverage of the origin");
        }
    }

    @Test
    @DisplayName("geoToH3Address and kRing agree about neighbor counts for an outer-ring cell")
    void kRingNeighborCountMatchesH3Property() {
        // Pick a non-pentagon cell.
        final H3CellAddress origin = service.geoToH3Address(40.7128, -74.0060, 9);
        // Non-pentagon cells have exactly 6 neighbors; the kRing count
        // (including origin) is therefore 7.
        final List<H3CellAddress> ring1 = service.kRing(origin, 1);
        assertEquals(7, ring1.size());

        // The hexRing at distance 1 contains exactly 6 cells, none of
        // which are the origin.
        final List<H3CellAddress> firstRing = service.hexRing(origin, 1);
        assertEquals(6, firstRing.size());
        for (final H3CellAddress c : firstRing) {
            assertFalse(c.address().equals(origin.address()));
        }
    }

    @Test
    @DisplayName("cellsCoveredBy is a set view over clusterByCell keys")
    void cellsCoveredByIsConsistentWithClusters() {
        final List<AuditGeoEvent> events = generateAuditEventsAround(
                new GeoCoordinate(40.7128, -74.0060), 0.01, 50);
        final Map<H3CellAddress, List<AuditGeoEvent>> clusters = service.clusterByCell(events, 9);
        final Set<H3CellAddress> cells = service.cellsCoveredBy(events, 9);
        assertEquals(clusters.keySet().size(), cells.size());
    }

    private AuditGeoEvent geoEvent(final String id, final double lat, final double lng) {
        final AuditEvent event = AuditEvent.of("actor", "booking.created",
                Map.of("eventId", id));
        return new AuditGeoEvent(event, new GeoCoordinate(lat, lng));
    }

    /**
     * Generates a deterministic set of audit events scattered around the
     * given anchor within a {@code +/-} range in both lat and lng. The
     * distribution is a uniform 5x5 lattice so the test is reproducible.
     */
    private List<AuditGeoEvent> generateAuditEventsAround(
            final GeoCoordinate anchor, final double range, final int count) {
        final List<AuditGeoEvent> out = new ArrayList<>(count);
        final int side = (int) Math.ceil(Math.sqrt(count));
        int emitted = 0;
        for (int i = 0; i < side && emitted < count; i++) {
            for (int j = 0; j < side && emitted < count; j++) {
                final double lat = anchor.getLatitude()
                        - range + (2.0 * range * i) / side;
                final double lng = anchor.getLongitude()
                        - range + (2.0 * range * j) / side;
                out.add(geoEvent("nyc-" + emitted, lat, lng));
                emitted++;
            }
        }
        return out;
    }
}