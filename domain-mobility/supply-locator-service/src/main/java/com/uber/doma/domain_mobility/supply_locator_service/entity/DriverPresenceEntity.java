package com.uber.doma.domain_mobility.supply_locator_service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "driver_presence")
public class DriverPresenceEntity {

    @Id
    private String driverId;

    @Column(nullable = false)
    private String status; // ONLINE, BUSY, OFFLINE

    @Version
    private Long version;

    public DriverPresenceEntity() {}

    public DriverPresenceEntity(String driverId, String status) {
        this.driverId = driverId;
        this.status = status;
    }

    public String getDriverId() { return driverId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getVersion() { return version; }
}
