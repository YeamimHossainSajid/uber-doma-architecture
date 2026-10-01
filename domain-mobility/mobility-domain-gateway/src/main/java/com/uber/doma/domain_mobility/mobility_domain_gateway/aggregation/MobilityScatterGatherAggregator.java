package com.uber.doma.domain_mobility.mobility_domain_gateway.aggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class MobilityScatterGatherAggregator {
    private static final Logger log = LoggerFactory.getLogger(MobilityScatterGatherAggregator.class);

    public Mono<MobilityRideOffer> assembleOffer(String riderId, double lat, double lng) {
        Mono<String> driverMatchMono = Mono.just("driver-9842")
            .delayElement(Duration.ofMillis(35));

        Mono<Double> surgeMono = Mono.just(1.35)
            .delayElement(Duration.ofMillis(20));

        Mono<Integer> etaMono = Mono.just(4)
            .delayElement(Duration.ofMillis(25));

        return Mono.zip(driverMatchMono, surgeMono, etaMono)
            .timeout(Duration.ofMillis(200))
            .map(tuple -> {
                String driverId = tuple.getT1();
                double surge = tuple.getT2();
                int eta = tuple.getT3();
                long fare = (long) (1250 * surge);

                log.info("[MobilityGateway] Assembled offer for rider {} in parallel: Driver={}, Surge={}x, ETA={}m",
                    riderId, driverId, surge, eta);

                return new MobilityRideOffer(riderId, driverId, surge, eta, fare);
            })
            .onErrorReturn(new MobilityRideOffer(riderId, "unassigned", 1.0, 10, 1250));
    }
}
