package com.uber.doma.domain_mobility.dispatch_coordinator_service.logging;

import org.slf4j.MDC;

public class DiscoMdcScope implements AutoCloseable {
    public static final String TRIP_KEY = "tripId";
    public static final String BATCH_ROUND_KEY = "batchRoundId";

    public DiscoMdcScope(String tripId, String batchRoundId) {
        MDC.put(TRIP_KEY, tripId);
        MDC.put(BATCH_ROUND_KEY, batchRoundId);
    }

    @Override
    public void close() {
        MDC.remove(TRIP_KEY);
        MDC.remove(BATCH_ROUND_KEY);
    }
}
