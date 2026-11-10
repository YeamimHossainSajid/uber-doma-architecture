package com.uber.doma.domain_platform.audit_compliance_service.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Binds the {@value #TRACE_ID_HEADER} and {@value #RIDER_ID_HEADER} request
 * headers into the SLF4J {@link MDC} so every log line written while a request
 * is being handled carries them as structured fields.
 *
 * <p>The values go into their own MDC keys ({@value #MDC_REQUEST_TRACE_ID},
 * {@value #MDC_RIDER_ID}) rather than {@code traceId}, which Micrometer Tracing
 * and {@code TraceContextMdcBinder} own and rewrite as spans open and close.</p>
 *
 * <p>Header values come from callers, so they are only accepted when they are
 * short and drawn from a conservative character set; anything else is treated
 * as absent. This keeps CR/LF and other log-forging input out of the logs.</p>
 */
public final class RequestCorrelationMdc {

    /** Inbound/outbound header carrying the caller's correlation trace ID. */
    public static final String TRACE_ID_HEADER = "X-Trace-ID";

    /** Inbound header carrying the rider the request acts on behalf of. */
    public static final String RIDER_ID_HEADER = "X-Rider-ID";

    /** MDC key for the {@value #TRACE_ID_HEADER} value. */
    public static final String MDC_REQUEST_TRACE_ID = "requestTraceId";

    /** MDC key for the {@value #RIDER_ID_HEADER} value. */
    public static final String MDC_RIDER_ID = "riderId";

    /** Longest header value accepted into the MDC. */
    public static final int MAX_VALUE_LENGTH = 128;

    private static final Pattern ALLOWED_VALUE = Pattern.compile("[A-Za-z0-9._:-]+");

    private RequestCorrelationMdc() {
    }

    /**
     * @param raw header value as received; may be {@code null}
     * @return the trimmed value, or {@code null} if it is blank, longer than
     *         {@value #MAX_VALUE_LENGTH} characters, or contains characters
     *         outside {@code [A-Za-z0-9._:-]}
     */
    public static String sanitize(final String raw) {
        if (raw == null) {
            return null;
        }
        final String value = raw.trim();
        if (value.isEmpty() || value.length() > MAX_VALUE_LENGTH || !ALLOWED_VALUE.matcher(value).matches()) {
            return null;
        }
        return value;
    }

    /**
     * Picks the trace ID to log for a request: the sanitized header value if
     * usable, otherwise the active OpenTelemetry trace ID, otherwise a freshly
     * generated 32-hex-character ID.
     *
     * @param headerValue raw {@value #TRACE_ID_HEADER} value; may be {@code null}
     * @return a non-null trace ID
     */
    public static String resolveTraceId(final String headerValue) {
        final String fromHeader = sanitize(headerValue);
        if (fromHeader != null) {
            return fromHeader;
        }
        final SpanContext current = Span.current().getSpanContext();
        if (current.isValid()) {
            return current.getTraceId();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Puts the correlation values into the MDC, removing a key whose value is
     * {@code null}.
     *
     * @return the previous values, for {@link #restore(Snapshot)}
     */
    public static Snapshot bind(final String requestTraceId, final String riderId) {
        final Snapshot previous = new Snapshot(MDC.get(MDC_REQUEST_TRACE_ID), MDC.get(MDC_RIDER_ID));
        put(MDC_REQUEST_TRACE_ID, requestTraceId);
        put(MDC_RIDER_ID, riderId);
        return previous;
    }

    /**
     * Restores the MDC entries captured by {@link #bind(String, String)}.
     */
    public static void restore(final Snapshot previous) {
        put(MDC_REQUEST_TRACE_ID, previous.requestTraceId());
        put(MDC_RIDER_ID, previous.riderId());
    }

    private static void put(final String key, final String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    /** MDC correlation values in place before a {@link #bind(String, String)}. */
    public record Snapshot(String requestTraceId, String riderId) {
    }
}
