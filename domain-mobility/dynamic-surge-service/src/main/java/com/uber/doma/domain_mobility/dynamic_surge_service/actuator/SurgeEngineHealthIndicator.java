package com.uber.doma.domain_mobility.dynamic_surge_service.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SurgeEngineHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
            .withDetail("surgeEngine", "ACTIVE")
            .withDetail("activeHexSurgeCount", 342)
            .build();
    }
}
