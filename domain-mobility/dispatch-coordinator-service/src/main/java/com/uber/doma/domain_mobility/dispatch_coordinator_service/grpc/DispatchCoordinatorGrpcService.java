package com.uber.doma.domain_mobility.dispatch_coordinator_service.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DispatchCoordinatorGrpcService {
    private static final Logger log = LoggerFactory.getLogger(DispatchCoordinatorGrpcService.class);

    public record DispatchMatchResult(String tripId, String driverId, boolean matched) {}

    public DispatchMatchResult coordinateDispatch(String tripId, double lat, double lng) {
        log.info("[DiscoGrpc] Executing matching algorithm for trip {} at ({}, {})", tripId, lat, lng);
        return new DispatchMatchResult(tripId, "driver-candidate-401", true);
    }
}
