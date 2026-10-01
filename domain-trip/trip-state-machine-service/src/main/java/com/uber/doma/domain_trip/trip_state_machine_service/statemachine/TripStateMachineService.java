package com.uber.doma.domain_trip.trip_state_machine_service.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class TripStateMachineService {
    private static final Logger log = LoggerFactory.getLogger(TripStateMachineService.class);

    private static final Map<TripState, Set<TripState>> VALID_TRANSITIONS = Map.of(
        TripState.REQUESTED, Set.of(TripState.MATCHING, TripState.CANCELLED),
        TripState.MATCHING, Set.of(TripState.DRIVER_ASSIGNED, TripState.CANCELLED),
        TripState.DRIVER_ASSIGNED, Set.of(TripState.DRIVER_ARRIVING, TripState.CANCELLED),
        TripState.DRIVER_ARRIVING, Set.of(TripState.IN_TRIP, TripState.CANCELLED),
        TripState.IN_TRIP, Set.of(TripState.COMPLETED),
        TripState.COMPLETED, Set.of(),
        TripState.CANCELLED, Set.of()
    );

    public TripState transition(String tripId, TripState current, TripState target) {
        Set<TripState> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new IllegalStateException(String.format(
                "Illegal Trip Transition for %s: Cannot move from %s to %s", tripId, current, target
            ));
        }

        log.info("[TripStateMachine] Trip {} transition successful: {} -> {}", tripId, current, target);
        return target;
    }
}
