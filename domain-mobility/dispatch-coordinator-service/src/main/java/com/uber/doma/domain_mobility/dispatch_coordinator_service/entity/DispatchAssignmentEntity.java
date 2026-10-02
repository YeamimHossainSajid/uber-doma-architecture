package com.uber.doma.domain_mobility.dispatch_coordinator_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dispatch_assignments")
public class DispatchAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tripId;

    @Column(nullable = false)
    private String driverId;

    @Column(nullable = false)
    private String status; // OFFERED, ACCEPTED, DECLINED

    @Column(nullable = false)
    private Instant matchedAt;

    public DispatchAssignmentEntity() {}

    public DispatchAssignmentEntity(String tripId, String driverId, String status) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.status = status;
        this.matchedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTripId() { return tripId; }
    public String getDriverId() { return driverId; }
    public String getStatus() { return status; }
    public Instant getMatchedAt() { return matchedAt; }
}
