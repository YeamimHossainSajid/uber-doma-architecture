package com.uber.doma.domain_mobility.demand_pin_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DemandPinServiceTest {

    @Test
    @DisplayName("Should execute domain logic without throwing exceptions")
    void testProcessDomainWork() {
        DemandPinService service = new DemandPinService();
        assertDoesNotThrow(service::processDomainWork);
    }
}
