package com.uber.doma.domain_driver.vehicle_registry_service.model;

public enum VehicleClass {
    UBER_X(4, 2, 2010),
    UBER_XL(6, 4, 2012),
    UBER_COMFORT(4, 3, 2017),
    UBER_BLACK(4, 3, 2019),
    UBER_GREEN(4, 2, 2015),
    WAV(4, 2, 2012);

    private final int minPassengerCapacity;
    private final int minLuggageCapacity;
    private final int minModelYear;

    VehicleClass(int minPassengerCapacity, int minLuggageCapacity, int minModelYear) {
        this.minPassengerCapacity = minPassengerCapacity;
        this.minLuggageCapacity = minLuggageCapacity;
        this.minModelYear = minModelYear;
    }

    public int getMinPassengerCapacity() { return minPassengerCapacity; }
    public int getMinLuggageCapacity() { return minLuggageCapacity; }
    public int getMinModelYear() { return minModelYear; }
}
