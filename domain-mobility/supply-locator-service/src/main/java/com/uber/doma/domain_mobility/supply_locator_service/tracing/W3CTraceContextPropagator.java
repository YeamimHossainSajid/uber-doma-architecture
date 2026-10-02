package com.uber.doma.domain_mobility.supply_locator_service.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class W3CTraceContextPropagator {
    private static final Logger log = LoggerFactory.getLogger(W3CTraceContextPropagator.class);
    public static final String TRACEPARENT_HEADER = "traceparent";

    public String generateTraceparent(String traceId, String spanId) {
        String header = String.format("00-%s-%s-01", traceId, spanId);
        log.info("[W3CTracing] Injected W3C traceparent: {}", header);
        return header;
    }
}
