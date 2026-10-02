package com.uber.doma.domain_mobility.demand_pin_service.reactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class ReactiveDemandAggregator {
    private static final Logger log = LoggerFactory.getLogger(ReactiveDemandAggregator.class);

    public record RegionalDemandProfile(int currentPins, double surgeIndex) {}

    public Mono<RegionalDemandProfile> fetchRegionalDemand(String hexId) {
        Mono<Integer> pinCountMono = Mono.just(142).delayElement(Duration.ofMillis(15));
        Mono<Double> surgeMono = Mono.just(1.4).delayElement(Duration.ofMillis(20));

        return Mono.zip(pinCountMono, surgeMono)
            .map(t -> new RegionalDemandProfile(t.getT1(), t.getT2()))
            .timeout(Duration.ofMillis(80));
    }
}
