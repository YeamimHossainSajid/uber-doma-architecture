package com.uber.doma.domain_mobility.supply_locator_service.logging;

import org.slf4j.MDC;

public class SupplyMdcContextScope implements AutoCloseable {
    public static final String TRACE_KEY = "traceId";
    public static final String DRIVER_KEY = "driverId";

    public SupplyMdcContextScope(String traceId, String driverId) {
        MDC.put(TRACE_KEY, traceId);
        MDC.put(DRIVER_KEY, driverId);
    }

    @Override
    public void close() {
        MDC.remove(TRACE_KEY);
        MDC.remove(DRIVER_KEY);
    }
}
