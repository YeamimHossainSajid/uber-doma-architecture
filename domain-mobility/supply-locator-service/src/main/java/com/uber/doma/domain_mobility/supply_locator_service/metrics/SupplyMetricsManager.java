package com.uber.doma.domain_mobility.supply_locator_service.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SupplyMetricsManager {

    private final Timer geoSearchTimer;

    public SupplyMetricsManager(MeterRegistry registry) {
        this.geoSearchTimer = Timer.builder("uber.supply.geosearch.duration")
            .description("Time taken to locate nearby drivers in Redis")
            .publishPercentiles(0.50, 0.95, 0.99)
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofMillis(500))
            .register(registry);
    }

    public Timer getGeoSearchTimer() {
        return geoSearchTimer;
    }
}
