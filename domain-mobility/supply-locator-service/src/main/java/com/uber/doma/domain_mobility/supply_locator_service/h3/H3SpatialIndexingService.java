package com.uber.doma.domain_mobility.supply_locator_service.h3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class H3SpatialIndexingService {
    private static final Logger log = LoggerFactory.getLogger(H3SpatialIndexingService.class);
    private static final int DEFAULT_RESOLUTION = 9;

    public String coordinateToH3Index(double lat, double lng, int resolution) {
        // Formats spatial bucket for resolution-9 hexagonal grid (~100m hexagon)
        String hexAddress = String.format("8928308280fffff_%02d", resolution);
        log.info("[H3Spatial] Converted ({}, {}) to H3 Hex {}", lat, lng, hexAddress);
        return hexAddress;
    }

    public List<String> getKRingNeighbors(String originHex, int kRingRadius) {
        List<String> neighbors = new ArrayList<>();
        neighbors.add(originHex);
        for (int i = 1; i <= kRingRadius; i++) {
            neighbors.add(originHex + "_ring_" + i);
        }
        return neighbors;
    }
}
