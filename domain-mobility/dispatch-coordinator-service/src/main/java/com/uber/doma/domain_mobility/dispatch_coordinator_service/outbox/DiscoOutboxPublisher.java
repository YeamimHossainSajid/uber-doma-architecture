package com.uber.doma.domain_mobility.dispatch_coordinator_service.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(DiscoOutboxPublisher.class);

    @Transactional
    public void publishDispatchEvent(String tripId, String driverId) {
        log.info("[DiscoOutbox] Staged DriverDispatchedEvent for Trip {} -> Driver {}", tripId, driverId);
    }
}
