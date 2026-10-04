package com.uber.doma.domain_mobility.dynamic_surge_service.logging;

import org.slf4j.MDC;

public class SurgeMdcScope implements AutoCloseable {
    public static final String HEX_KEY = "hexId";
    public static final String SURGE_KEY = "surgeMultiplier";

    public SurgeMdcScope(String hexId, double multiplier) {
        MDC.put(HEX_KEY, hexId);
        MDC.put(SURGE_KEY, String.valueOf(multiplier));
    }

    @Override
    public void close() {
        MDC.remove(HEX_KEY);
        MDC.remove(SURGE_KEY);
    }
}
