package com.uber.doma.domain_mobility.dispatch_coordinator_service.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DispatchEngineHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
            .withDetail("discoEngine", "OPTIMAL")
            .withDetail("activeBatchRoundsPerMinute", 120)
            .build();
    }
}
