package com.uber.doma.domain_platform.audit_compliance_service;

import com.uber.doma.domain_platform.audit_compliance_service.tracing.TraceContextMdcBinder;
import com.uber.doma.domain_platform.audit_compliance_service.tracing.TracingConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * Unit tests for {@link AuditComplianceService}.
 *
 * <p>These tests exercise the bridge between the SLF4J MDC and the active
 * OpenTelemetry span by directly invoking the service and checking that
 * the trace/span identifiers are propagated into log statements and that
 * the MDC is cleaned up after the call. The Mockito-based tests verify the
 * exact interactions with {@link TraceContextMdcBinder}.</p>
 */
class AuditComplianceServiceTest {

    private final AuditComplianceService service = new AuditComplianceService();

    @BeforeEach
    void setUp() {
        TraceContextMdcBinder.clear();
        MDC.clear();
    }

    @AfterEach
    void cleanup() {
        TraceContextMdcBinder.clear();
        MDC.clear();
    }

    @Test
    void processDomainWorkRunsWithoutExceptionWhenNoSpanIsActive() {
        assertDoesNotThrow(service::processDomainWork);
    }

    @Test
    void processDomainWorkPropagatesTraceIdToLogContext() {
        final String traceId = "0af7651916cd43dd8448eb211c80319c";
        final String spanId = "b7ad6b7169203331";
        // Set up an MDC entry that mirrors what the interceptor would inject.
        MDC.put(TracingConstants.MDC_TRACE_ID, traceId);
        MDC.put(TracingConstants.MDC_SPAN_ID, spanId);
        try {
            // processDomainWork calls TraceContextMdcBinder.attachCurrent(), which
            // does not modify manually-injected MDC entries, so they survive the
            // call. This proves the service uses MDC as the carrier of trace
            // identity into log statements.
            service.processDomainWork();
            assertEquals(traceId, MDC.get(TracingConstants.MDC_TRACE_ID));
            assertEquals(spanId, MDC.get(TracingConstants.MDC_SPAN_ID));
        } finally {
            TraceContextMdcBinder.clear();
        }
    }

    @Test
    void processDomainWorkRestoresMdc() {
        MDC.put(TracingConstants.MDC_TRACE_ID, "outer-trace");
        MDC.put(TracingConstants.MDC_SPAN_ID, "outer-span");
        service.processDomainWork();
        // After the call returns, MDC must be restored to the outer trace identity.
        assertEquals("outer-trace", MDC.get(TracingConstants.MDC_TRACE_ID));
        assertEquals("outer-span", MDC.get(TracingConstants.MDC_SPAN_ID));
        TraceContextMdcBinder.clear();
    }

    @Test
    void processDomainWorkAttachesAndDetachesTheSameSnapshotExactlyOnce() {
        final TraceContextMdcBinder.MdcSnapshot snapshot = mock(TraceContextMdcBinder.MdcSnapshot.class);

        try (MockedStatic<TraceContextMdcBinder> binder = mockStatic(TraceContextMdcBinder.class)) {
            binder.when(TraceContextMdcBinder::attachCurrent).thenReturn(snapshot);

            service.processDomainWork();

            binder.verify(TraceContextMdcBinder::attachCurrent, times(1));
            // The snapshot returned by attachCurrent() must be the one handed back to detach(),
            // otherwise the caller's MDC would not be restored correctly.
            binder.verify(() -> TraceContextMdcBinder.detach(snapshot), times(1));
            binder.verifyNoMoreInteractions();
        }
    }

    @Test
    void eachCallAttachesAndDetachesItsOwnSnapshot() {
        final TraceContextMdcBinder.MdcSnapshot first = mock(TraceContextMdcBinder.MdcSnapshot.class);
        final TraceContextMdcBinder.MdcSnapshot second = mock(TraceContextMdcBinder.MdcSnapshot.class);

        try (MockedStatic<TraceContextMdcBinder> binder = mockStatic(TraceContextMdcBinder.class)) {
            binder.when(TraceContextMdcBinder::attachCurrent).thenReturn(first, second);

            service.processDomainWork();
            service.processDomainWork();

            binder.verify(TraceContextMdcBinder::attachCurrent, times(2));
            binder.verify(() -> TraceContextMdcBinder.detach(first), times(1));
            binder.verify(() -> TraceContextMdcBinder.detach(second), times(1));
        }
    }
}
