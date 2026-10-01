package com.uber.doma.domain_mobility.dynamic_surge_service.surge;

import java.time.Instant;

public record SurgeQuote(
    String hexId,
    double multiplier,
    int supplyCount,
    int demandCount,
    Instant expiresAt
) {}
