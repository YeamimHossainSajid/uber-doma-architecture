package com.uber.doma.domain_mobility.supply_locator_service.geo;

import java.time.Instant;

public record DriverLocation(
    String driverId,
    double latitude,
    double longitude,
    String vehicleClass,
    Instant timestamp
) {}
