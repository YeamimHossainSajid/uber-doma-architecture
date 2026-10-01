package com.uber.doma.domain_trip.trip_state_machine_service.statemachine;

public enum TripState {
    REQUESTED,
    MATCHING,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVING,
    IN_TRIP,
    COMPLETED,
    CANCELLED
}
