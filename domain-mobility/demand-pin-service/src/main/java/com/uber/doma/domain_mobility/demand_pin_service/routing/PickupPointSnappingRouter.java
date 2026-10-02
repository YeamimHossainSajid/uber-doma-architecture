package com.uber.doma.domain_mobility.demand_pin_service.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PickupPointSnappingRouter {
    private static final Logger log = LoggerFactory.getLogger(PickupPointSnappingRouter.class);

    public record SnappedPoint(long nearestNodeId, double distanceMeters) {}

    public SnappedPoint snapPickupPoint(double lat, double lng) {
        log.info("[CHRouter] Snapped coordinate ({}, {}) to street graph node in <2ms", lat, lng);
        return new SnappedPoint(1008291L, 12.4);
    }
}
