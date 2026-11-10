package com.uber.doma.domain_platform.audit_compliance_service.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationMdcTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void sanitizeTrimsAndKeepsValidValues() {
        assertThat(RequestCorrelationMdc.sanitize("  rider-42_a.b:c  ")).isEqualTo("rider-42_a.b:c");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc\r\nFAKE LOG LINE", "has space", "quote\"inside", "{json}", "naïve"})
    void sanitizeRejectsBlankOrUnsafeValues(final String raw) {
        assertThat(RequestCorrelationMdc.sanitize(raw)).isNull();
    }

    @Test
    void sanitizeEnforcesMaximumLength() {
        final String atLimit = "a".repeat(RequestCorrelationMdc.MAX_VALUE_LENGTH);
        assertThat(RequestCorrelationMdc.sanitize(atLimit)).isEqualTo(atLimit);
        assertThat(RequestCorrelationMdc.sanitize(atLimit + "a")).isNull();
    }

    @Test
    void resolveTraceIdPrefersHeaderValue() {
        assertThat(RequestCorrelationMdc.resolveTraceId("trace-123")).isEqualTo("trace-123");
    }

    @Test
    void resolveTraceIdFallsBackToActiveSpan() {
        final SpanContext spanContext = SpanContext.create(
                "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7",
                TraceFlags.getSampled(), TraceState.getDefault());
        try (Scope ignored = Context.root().with(Span.wrap(spanContext)).makeCurrent()) {
            assertThat(RequestCorrelationMdc.resolveTraceId("bad value")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        }
    }

    @Test
    void resolveTraceIdGeneratesIdWhenNothingElseAvailable() {
        final String generated = RequestCorrelationMdc.resolveTraceId(null);
        assertThat(generated).matches("[0-9a-f]{32}");
        assertThat(RequestCorrelationMdc.resolveTraceId(null)).isNotEqualTo(generated);
    }

    @Test
    void bindAndRestoreRoundTripPreviousValues() {
        MDC.put(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID, "outer-trace");

        final RequestCorrelationMdc.Snapshot previous = RequestCorrelationMdc.bind("inner-trace", "rider-7");
        assertThat(MDC.get(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID)).isEqualTo("inner-trace");
        assertThat(MDC.get(RequestCorrelationMdc.MDC_RIDER_ID)).isEqualTo("rider-7");

        RequestCorrelationMdc.restore(previous);
        assertThat(MDC.get(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID)).isEqualTo("outer-trace");
        assertThat(MDC.get(RequestCorrelationMdc.MDC_RIDER_ID)).isNull();
    }

    @Test
    void bindRemovesKeysForNullValues() {
        MDC.put(RequestCorrelationMdc.MDC_RIDER_ID, "stale-rider");

        RequestCorrelationMdc.bind("trace", null);

        assertThat(MDC.get(RequestCorrelationMdc.MDC_RIDER_ID)).isNull();
    }
}
