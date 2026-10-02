package com.uber.doma.domain_platform.audit_compliance_service.resilience;

/**
 * Synthetic exception thrown by fault-injection stubs when a "packet loss"
 * or "network partition" event is simulated. The exception extends
 * {@link RuntimeException} so it cleanly propagates out of the
 * {@link UpstreamAuditChannel} contract methods without forcing every
 * caller to declare checked exceptions.
 */
public class SimulatedPartitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SimulatedPartitionException(final String message) {
        super(message);
    }
}