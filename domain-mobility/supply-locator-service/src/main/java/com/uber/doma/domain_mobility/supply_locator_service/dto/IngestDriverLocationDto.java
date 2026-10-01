package com.uber.doma.domain_mobility.supply_locator_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngestDriverLocationDto(
    @NotBlank(message = "driverId cannot be empty")
    String driverId,

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    Double latitude,

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    Double longitude,

    @NotBlank(message = "vehicleClass is required")
    String vehicleClass
) {}
