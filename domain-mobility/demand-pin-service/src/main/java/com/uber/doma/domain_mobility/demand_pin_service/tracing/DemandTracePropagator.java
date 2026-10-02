package com.uber.doma.domain_mobility.demand_pin_service.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemandTracePropagator {
    private static final Logger log = LoggerFactory.getLogger(DemandTracePropagator.class);

    public String formatTraceparent(String traceId, String spanId) {
        String header = String.format("00-%s-%s-01", traceId, spanId);
        log.info("[W3C] Attached traceparent header to demand ping: {}", header);
        return header;
    }
}
