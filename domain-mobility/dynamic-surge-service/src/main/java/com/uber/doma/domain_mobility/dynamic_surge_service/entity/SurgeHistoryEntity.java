package com.uber.doma.domain_mobility.dynamic_surge_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "surge_history")
public class SurgeHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hexId;

    @Column(nullable = false)
    private Double multiplier;

    @Column(nullable = false)
    private Instant recordedAt;

    public SurgeHistoryEntity() {}

    public SurgeHistoryEntity(String hexId, Double multiplier) {
        this.hexId = hexId;
        this.multiplier = multiplier;
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getHexId() { return hexId; }
    public Double getMultiplier() { return multiplier; }
    public Instant getRecordedAt() { return recordedAt; }
}
