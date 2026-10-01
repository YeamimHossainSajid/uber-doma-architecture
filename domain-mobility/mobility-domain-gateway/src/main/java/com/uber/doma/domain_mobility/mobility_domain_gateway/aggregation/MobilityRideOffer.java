package com.uber.doma.domain_mobility.mobility_domain_gateway.aggregation;

public record MobilityRideOffer(
    String riderId,
    String driverId,
    double surgeMultiplier,
    int etaMinutes,
    long totalFareCents
) {}
