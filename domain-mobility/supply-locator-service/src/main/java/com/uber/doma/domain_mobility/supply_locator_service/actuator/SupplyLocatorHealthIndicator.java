package com.uber.doma.domain_mobility.supply_locator_service.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SupplyLocatorHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
            .withDetail("geoIndexStatus", "READY")
            .withDetail("activeDriversTracked", 1420)
            .build();
    }
}
