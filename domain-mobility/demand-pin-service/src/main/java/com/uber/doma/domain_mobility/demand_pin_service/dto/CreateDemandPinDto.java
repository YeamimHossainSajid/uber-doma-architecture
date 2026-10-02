package com.uber.doma.domain_mobility.demand_pin_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDemandPinDto(
    @NotBlank(message = "riderId is required")
    String riderId,

    @NotNull(message = "latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    Double pickupLat,

    @NotNull(message = "longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    Double pickupLng
) {}
