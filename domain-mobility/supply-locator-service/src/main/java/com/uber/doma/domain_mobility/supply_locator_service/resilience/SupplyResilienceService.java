package com.uber.doma.domain_mobility.supply_locator_service.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SupplyResilienceService {
    private static final Logger log = LoggerFactory.getLogger(SupplyResilienceService.class);

    public List<String> fallbackNearbyDrivers(Throwable t) {
        log.warn("[CircuitBreaker] Primary driver locator degraded. Engaging circuit fallback: {}", t.getMessage());
        return Collections.emptyList();
    }
}
