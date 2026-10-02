package com.uber.doma.domain_mobility.dispatch_coordinator_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DispatchCoordinatorServiceTest {

    @Test
    @DisplayName("Should execute DISCO matching logic without throwing exceptions")
    void testProcessDomainWork() {
        DispatchCoordinatorService service = new DispatchCoordinatorService();
        assertDoesNotThrow(service::processDomainWork);
    }
}
