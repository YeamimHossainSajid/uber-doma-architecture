package com.uber.doma.domain_mobility.supply_locator_service.reactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class ReactiveSupplyAggregator {
    private static final Logger log = LoggerFactory.getLogger(ReactiveSupplyAggregator.class);

    public record AggregatedSupply(List<String> uberXDrivers, List<String> comfortDrivers) {}

    public Mono<AggregatedSupply> fetchMultiTierSupply(double lat, double lng) {
        Mono<List<String>> uberXMono = Mono.just(List.of("d-1", "d-2")).delayElement(Duration.ofMillis(20));
        Mono<List<String>> comfortMono = Mono.just(List.of("d-9")).delayElement(Duration.ofMillis(25));

        return Mono.zip(uberXMono, comfortMono)
            .map(t -> new AggregatedSupply(t.getT1(), t.getT2()))
            .timeout(Duration.ofMillis(100));
    }
}
