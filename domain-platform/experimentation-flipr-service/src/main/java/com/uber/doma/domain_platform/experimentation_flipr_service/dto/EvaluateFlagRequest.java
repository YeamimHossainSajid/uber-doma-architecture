package com.uber.doma.domain_platform.experimentation_flipr_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EvaluateFlagRequest {

    @NotBlank(message = "Flag key must not be blank")
    @Size(min = 1, max = 255, message = "Flag key must be between 1 and 255 characters")
    private String flagKey;

    @NotBlank(message = "Entity ID must not be blank")
    @Size(min = 1, max = 128, message = "Entity ID must be between 1 and 128 characters")
    private String entityId;

    @NotNull(message = "Rollout percentage must not be null")
    @Min(value = 0, message = "Rollout percentage must be at least 0")
    @DecimalMin(value = "0.0", message = "Rollout percentage must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Rollout percentage must not exceed 100.0")
    private Double rolloutPercentage;

    @Size(max = 64, message = "Variant must not exceed 64 characters")
    private String defaultVariant;

    public EvaluateFlagRequest() {
    }

    public EvaluateFlagRequest(String flagKey, String entityId, Double rolloutPercentage, String defaultVariant) {
        this.flagKey = flagKey;
        this.entityId = entityId;
        this.rolloutPercentage = rolloutPercentage;
        this.defaultVariant = defaultVariant;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public void setFlagKey(String flagKey) {
        this.flagKey = flagKey;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Double getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(Double rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    public String getDefaultVariant() {
        return defaultVariant;
    }

    public void setDefaultVariant(String defaultVariant) {
        this.defaultVariant = defaultVariant;
    }
}