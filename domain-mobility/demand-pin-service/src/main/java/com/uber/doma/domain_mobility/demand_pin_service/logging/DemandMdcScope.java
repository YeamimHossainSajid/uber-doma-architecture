package com.uber.doma.domain_mobility.demand_pin_service.logging;

import org.slf4j.MDC;

public class DemandMdcScope implements AutoCloseable {
    public static final String RIDER_KEY = "riderId";

    public DemandMdcScope(String riderId) {
        MDC.put(RIDER_KEY, riderId);
    }

    @Override
    public void close() {
        MDC.remove(RIDER_KEY);
    }
}
