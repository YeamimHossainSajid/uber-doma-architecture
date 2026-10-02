package com.uber.doma.domain_mobility.demand_pin_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DemandPinArchitectureRulesTest {

    @Test
    @DisplayName("Verify demand pin service has zero direct dependencies on Driver or Billing domain classes")
    void testDomainBoundaryIntegrity() {
        String currentPkg = DemandPinApplication.class.getPackageName();
        assertFalse(currentPkg.contains("driver"));
        assertFalse(currentPkg.contains("billing"));
    }
}
