package com.uber.doma.domain_mobility.supply_locator_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SupplyLocatorServiceTest {

    private SupplyLocatorService supplyLocatorService;

    @BeforeEach
    void setUp() {
        supplyLocatorService = new SupplyLocatorService();
    }

    @Test
    @DisplayName("Should successfully execute domain logic without throwing exceptions")
    void testProcessDomainWork() {
        assertDoesNotThrow(() -> supplyLocatorService.processDomainWork());
    }
}
