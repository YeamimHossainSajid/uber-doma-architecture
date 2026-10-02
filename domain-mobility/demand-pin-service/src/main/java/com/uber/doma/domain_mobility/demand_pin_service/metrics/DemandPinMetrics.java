package com.uber.doma.domain_mobility.demand_pin_service.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DemandPinMetrics {

    private final Timer pinDropTimer;

    public DemandPinMetrics(MeterRegistry registry) {
        this.pinDropTimer = Timer.builder("uber.demand.pindrop.duration")
            .description("Time taken to record rider pickup pin")
            .publishPercentiles(0.50, 0.95, 0.99)
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofMillis(200))
            .register(registry);
    }

    public Timer getPinDropTimer() {
        return pinDropTimer;
    }
}
