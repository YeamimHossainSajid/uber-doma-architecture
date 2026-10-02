package com.uber.doma.domain_mobility.demand_pin_service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemandPinDlqHandler {
    private static final Logger log = LoggerFactory.getLogger(DemandPinDlqHandler.class);
    public static final String DEMAND_DLQ_TOPIC = "mobility.demand-pin.dlq";

    public void handlePoisonPill(String payload, Exception e) {
        log.error("[DemandDLQ] Offloaded corrupt message to topic {}: {}", DEMAND_DLQ_TOPIC, e.getMessage());
    }
}
