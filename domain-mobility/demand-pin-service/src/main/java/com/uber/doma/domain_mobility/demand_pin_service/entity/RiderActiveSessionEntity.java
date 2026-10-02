package com.uber.doma.domain_mobility.demand_pin_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "rider_active_sessions")
public class RiderActiveSessionEntity {

    @Id
    private String riderId;

    private String activeSearchHex;

    @Version
    private Long version;

    public RiderActiveSessionEntity() {}

    public RiderActiveSessionEntity(String riderId, String activeSearchHex) {
        this.riderId = riderId;
        this.activeSearchHex = activeSearchHex;
    }

    public String getRiderId() { return riderId; }
    public String getActiveSearchHex() { return activeSearchHex; }
    public void setActiveSearchHex(String hex) { this.activeSearchHex = hex; }
    public Long getVersion() { return version; }
}
