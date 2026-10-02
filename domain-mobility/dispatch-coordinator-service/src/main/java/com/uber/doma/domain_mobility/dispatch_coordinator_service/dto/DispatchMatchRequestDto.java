package com.uber.doma.domain_mobility.dispatch_coordinator_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DispatchMatchRequestDto(
    @NotBlank(message = "tripId is required")
    String tripId,

    @NotBlank(message = "riderId is required")
    String riderId,

    @NotNull(message = "pickupLat is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    Double pickupLat,

    @NotNull(message = "pickupLng is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    Double pickupLng,

    @NotBlank(message = "vehicleClass is required")
    String vehicleClass
) {}
