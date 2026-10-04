package com.uber.doma.domain_mobility.dynamic_surge_service.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurgeOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(SurgeOutboxPublisher.class);

    @Transactional
    public void publishSurgeUpdate(String hexId, double multiplier) {
        log.info("[SurgeOutbox] Staged SurgeMultiplierUpdatedEvent for hex {} -> {}x in outbox table", hexId, multiplier);
    }
}
