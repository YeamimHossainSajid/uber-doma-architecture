package com.uber.doma.domain_trip.safety_telematics_service.telematics;

public record AccelerometerFrame(
    String tripId,
    double gForceX,
    double gForceY,
    double gForceZ,
    double currentSpeedMph,
    long timestampMs
) {}
