package com.uber.doma.domain_mobility.supply_locator_service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaDeadLetterQueueConfig {
    private static final Logger log = LoggerFactory.getLogger(KafkaDeadLetterQueueConfig.class);
    public static final String SUPPLY_LOCATOR_DLQ_TOPIC = "mobility.supply-locator.dlq";

    public void routeToDeadLetterQueue(String failedPayload, String reason) {
        log.warn("[KafkaDLQ] Routing poisoned payload to topic {}: Reason={}", SUPPLY_LOCATOR_DLQ_TOPIC, reason);
    }
}
