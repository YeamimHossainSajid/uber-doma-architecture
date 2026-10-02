package com.uber.doma.domain_mobility.demand_pin_service.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DemandPinGrpcService {
    private static final Logger log = LoggerFactory.getLogger(DemandPinGrpcService.class);

    public record IngestDemandResponse(String pinId, boolean accepted) {}

    public IngestDemandResponse recordPin(String riderId, double lat, double lng) {
        log.info("[DemandPinGrpc] Processed binary gRPC demand pin from rider {} ({}, {})", riderId, lat, lng);
        return new IngestDemandResponse("pin-" + System.currentTimeMillis(), true);
    }
}
