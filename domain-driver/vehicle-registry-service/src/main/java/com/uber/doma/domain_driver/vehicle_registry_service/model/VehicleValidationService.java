package com.uber.doma.domain_driver.vehicle_registry_service.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VehicleValidationService {
    private static final Logger log = LoggerFactory.getLogger(VehicleValidationService.class);

    public boolean isVehicleEligible(String vin, VehicleClass targetClass, int modelYear, int passengerCapacity) {
        if (modelYear < targetClass.getMinModelYear()) {
            log.warn("[VehicleRegistry] VIN {} disqualified from {}: Model year {} < {}", 
                vin, targetClass, modelYear, targetClass.getMinModelYear());
            return false;
        }

        if (passengerCapacity < targetClass.getMinPassengerCapacity()) {
            return false;
        }

        log.info("[VehicleRegistry] VIN {} approved for tier {}", vin, targetClass);
        return true;
    }
}
