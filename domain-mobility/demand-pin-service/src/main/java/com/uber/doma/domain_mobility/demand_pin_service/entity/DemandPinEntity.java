package com.uber.doma.domain_mobility.demand_pin_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "demand_pins")
public class DemandPinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String riderId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Instant createdAt;

    public DemandPinEntity() {}

    public DemandPinEntity(String riderId, Double latitude, Double longitude) {
        this.riderId = riderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRiderId() { return riderId; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Instant getCreatedAt() { return createdAt; }
}
