package com.uber.doma.domain_mobility.supply_locator_service.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SupplyLocatorGrpcService {
    private static final Logger log = LoggerFactory.getLogger(SupplyLocatorGrpcService.class);

    public record IngestLocationResult(String driverId, boolean success, long timestamp) {}

    public IngestLocationResult handleLocationPing(String driverId, double lat, double lng) {
        log.info("[SupplyLocatorGrpc] Ingested binary gRPC ping from driver {} at ({}, {})", driverId, lat, lng);
        return new IngestLocationResult(driverId, true, System.currentTimeMillis());
    }
}
