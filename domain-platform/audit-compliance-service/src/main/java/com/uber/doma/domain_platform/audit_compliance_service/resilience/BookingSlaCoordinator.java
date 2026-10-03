package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates a booking request with the audit-compliance-service under
 * degraded network conditions. The coordinator is the boundary at which
 * we guarantee the booking SLA: even when the audit sink is fully
 * partitioned and dropping 30% of write attempts, the booking request
 * itself must complete successfully within its latency budget.
 *
 * <p>The {@link #executeBooking} method is the chaos-tested hot path. It
 * records the audit event asynchronously (best-effort) and returns a
 * {@link BookingResult} indicating whether the audit was either persisted
 * upstream, queued locally, or had to be dropped due to buffer overflow.
 * The booking result is independent of the audit outcome — a dropped audit
 * event never causes a booking to fail.</p>
 */
@Component
public class BookingSlaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(BookingSlaCoordinator.class);

    private final ResilientAuditSink auditSink;
    private final AtomicLong bookingAccepted = new AtomicLong(0);
    private final AtomicLong bookingAuditDeferred = new AtomicLong(0);

    public BookingSlaCoordinator() {
        this(new ResilientAuditSink());
    }

    public BookingSlaCoordinator(final ResilientAuditSink auditSink) {
        this.auditSink = Objects.requireNonNull(auditSink, "auditSink");
    }

    /**
     * Executes a booking request and records the corresponding audit event.
     * The method never throws — every failure path returns a
     * {@link BookingResult} so the caller can complete the booking without
     * being blocked by an audit subsystem failure.
     */
    public BookingResult executeBooking(final String bookingId, final String actorId) {
        if (bookingId == null || bookingId.isBlank()) {
            return new BookingResult(bookingId, BookingOutcome.REJECTED, "missing bookingId");
        }
        if (actorId == null || actorId.isBlank()) {
            return new BookingResult(bookingId, BookingOutcome.REJECTED, "missing actorId");
        }
        bookingAccepted.incrementAndGet();
        final AuditEvent event = AuditEvent.of(actorId, "booking.created",
                java.util.Map.of("bookingId", bookingId));
        try {
            final boolean recorded = auditSink.record(event);
            if (recorded) {
                if (auditSink.findBuffered(event.getId()).isPresent()) {
                    bookingAuditDeferred.incrementAndGet();
                    log.info("[BookingSlaCoordinator] booking {} accepted; audit queued for replay",
                            bookingId);
                    return new BookingResult(bookingId, BookingOutcome.ACCEPTED_AUDIT_DEFERRED, event.getId());
                }
                log.info("[BookingSlaCoordinator] booking {} accepted; audit persisted upstream",
                        bookingId);
                return new BookingResult(bookingId, BookingOutcome.ACCEPTED_AUDIT_PERSISTED, event.getId());
            }
            // Buffer overflow: we still accept the booking but log that the
            // audit was dropped so the operations team can react.
            bookingAuditDeferred.incrementAndGet();
            log.warn("[BookingSlaCoordinator] booking {} accepted but audit dropped (buffer overflow)",
                    bookingId);
            return new BookingResult(bookingId, BookingOutcome.ACCEPTED_AUDIT_DROPPED, event.getId());
        } catch (final RuntimeException ex) {
            // Defense in depth — record() should already catch all exceptions,
            // but a single unhandled exception here would violate the SLA.
            bookingAuditDeferred.incrementAndGet();
            log.error("[BookingSlaCoordinator] unexpected error during audit for booking {}: {}",
                    bookingId, ex.getMessage(), ex);
            return new BookingResult(bookingId, BookingOutcome.ACCEPTED_AUDIT_DROPPED, event.getId());
        }
    }

    public long getBookingsAccepted() {
        return bookingAccepted.get();
    }

    public long getBookingsAuditDeferred() {
        return bookingAuditDeferred.get();
    }

    public ResilientAuditSink getAuditSink() {
        return auditSink;
    }

    /**
     * Outcome of a booking request.
     */
    public enum BookingOutcome {
        /** Booking accepted; audit event persisted upstream. */
        ACCEPTED_AUDIT_PERSISTED,
        /** Booking accepted; audit event queued locally for replay. */
        ACCEPTED_AUDIT_DEFERRED,
        /** Booking accepted; audit event dropped due to buffer overflow. */
        ACCEPTED_AUDIT_DROPPED,
        /** Booking rejected; the request itself was malformed. */
        REJECTED
    }

    /**
     * Result of a single booking execution.
     */
    public record BookingResult(String bookingId, BookingOutcome outcome, String auditEventId) {
    }
}