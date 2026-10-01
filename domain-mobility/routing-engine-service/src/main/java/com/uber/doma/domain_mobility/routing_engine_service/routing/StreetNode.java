package com.uber.doma.domain_mobility.routing_engine_service.routing;

public record StreetNode(
    long nodeId,
    double latitude,
    double longitude,
    int hierarchyRank
) {}
