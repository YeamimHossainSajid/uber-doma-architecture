package com.uber.doma.domain_mobility.dynamic_surge_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DynamicSurgeServiceTest {

    @Test
    @DisplayName("Should execute dynamic surge pricing work without throwing exceptions")
    void testProcessDomainWork() {
        DynamicSurgeService service = new DynamicSurgeService();
        assertDoesNotThrow(service::processDomainWork);
    }
}
