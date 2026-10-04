package com.uber.doma.domain_mobility.dynamic_surge_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SurgeQueryDto(
    @NotBlank(message = "hexId is required")
    String hexId,

    @NotNull(message = "latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    Double latitude,

    @NotNull(message = "longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    Double longitude
) {}
