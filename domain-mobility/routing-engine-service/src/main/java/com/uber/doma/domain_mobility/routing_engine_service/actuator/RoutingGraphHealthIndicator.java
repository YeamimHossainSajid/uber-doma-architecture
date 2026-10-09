package com.uber.doma.domain_mobility.routing_engine_service.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RoutingGraphHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
            .withDetail("streetGraphStatus", "LOADED")
            .withDetail("activeRoadNodes", 2450000)
            .build();
    }
}
