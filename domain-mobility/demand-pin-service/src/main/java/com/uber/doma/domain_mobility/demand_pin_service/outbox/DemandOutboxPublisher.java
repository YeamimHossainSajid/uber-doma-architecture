package com.uber.doma.domain_mobility.demand_pin_service.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemandOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(DemandOutboxPublisher.class);

    @Transactional
    public void publishDemandEvent(String riderId, double lat, double lng) {
        log.info("[DemandOutbox] Staged DemandPinCreatedEvent for rider {} at ({}, {}) in outbox", riderId, lat, lng);
    }
}
