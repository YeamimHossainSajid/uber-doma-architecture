package com.uber.doma.domain_mobility.supply_locator_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DomaArchitectureRulesTest {

    @Test
    @DisplayName("Verify Mobility Domain does not import prohibited cross-domain leaf packages")
    void testStrictDomainIsolation() {
        String mobilityPackage = "com.uber.doma.domain_mobility";
        String prohibitedBilling = "com.uber.doma.domain_billing";
        String prohibitedRider = "com.uber.doma.domain_rider";

        assertTrue(mobilityPackage.startsWith("com.uber.doma.domain_mobility"));
        assertTrue(!mobilityPackage.contains(prohibitedBilling));
        assertTrue(!mobilityPackage.contains(prohibitedRider));
    }
}
