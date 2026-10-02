package com.uber.doma.domain_mobility.demand_pin_service.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DemandPinCircuitBreakerConfig {
    private static final Logger log = LoggerFactory.getLogger(DemandPinCircuitBreakerConfig.class);

    public int fallbackDemandCount(String hexId, Throwable t) {
        log.warn("[CircuitBreaker] Demand aggregation degraded for hex {}. Falling back to default baseline: {}", hexId, t.getMessage());
        return 10;
    }
}
