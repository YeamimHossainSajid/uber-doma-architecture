package com.uber.doma.domain_mobility.dynamic_surge_service.surge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SurgeCalculationService {
    private static final Logger log = LoggerFactory.getLogger(SurgeCalculationService.class);
    private static final double MIN_SURGE = 1.0;
    private static final double MAX_SURGE = 4.5;
    private static final double SENSITIVITY_ALPHA = 0.25;

    public SurgeQuote calculateSurge(String hexId, int supplyCount, int demandCount) {
        double rawSurge = MIN_SURGE;
        if (supplyCount == 0 && demandCount > 0) {
            rawSurge = MAX_SURGE;
        } else if (supplyCount > 0) {
            double ratio = (double) demandCount / supplyCount;
            if (ratio > 1.2) {
                rawSurge = MIN_SURGE + (ratio - 1.2) * SENSITIVITY_ALPHA;
            }
        }

        double clampedSurge = Math.round(Math.min(MAX_SURGE, Math.max(MIN_SURGE, rawSurge)) * 10.0) / 10.0;
        Instant expiresAt = Instant.now().plusSeconds(120);

        log.info("[DynamicSurge] Hex {}: Supply={}, Demand={} -> Surge Multiplier {}x",
            hexId, supplyCount, demandCount, clampedSurge);

        return new SurgeQuote(hexId, clampedSurge, supplyCount, demandCount, expiresAt);
    }
}
