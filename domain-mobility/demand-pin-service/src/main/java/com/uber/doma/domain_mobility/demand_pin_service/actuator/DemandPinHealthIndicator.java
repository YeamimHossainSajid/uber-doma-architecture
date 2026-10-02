package com.uber.doma.domain_mobility.demand_pin_service.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DemandPinHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
            .withDetail("demandBufferStatus", "HEALTHY")
            .withDetail("currentDemandRatePerSec", 840)
            .build();
    }
}
