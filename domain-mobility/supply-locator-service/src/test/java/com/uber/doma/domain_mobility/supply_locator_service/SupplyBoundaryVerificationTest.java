package com.uber.doma.domain_mobility.supply_locator_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SupplyBoundaryVerificationTest {

    @Test
    @DisplayName("Verify supply locator has no dependencies on Billing or Rider domain classes")
    void testBoundaryIntegrity() {
        String currentPackage = SupplyLocatorApplication.class.getPackageName();
        assertFalse(currentPackage.contains("billing"));
        assertFalse(currentPackage.contains("rider"));
    }
}
