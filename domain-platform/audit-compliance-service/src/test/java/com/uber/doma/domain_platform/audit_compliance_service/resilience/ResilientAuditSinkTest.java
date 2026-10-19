package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Mockito-based unit tests for {@link ResilientAuditSink}. The upstream
 * {@link UpstreamAuditChannel} is mocked so each branch of the sink
 * (persist, buffer, overflow, replay, local read fallback) can be driven
 * deterministically and its interactions verified.
 */
@ExtendWith(MockitoExtension.class)
class ResilientAuditSinkTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UpstreamAuditChannel channel;

    private static AuditEvent event(final String id, final String actorId) {
        return new AuditEvent(id, actorId, "TRIP_BOOKED", NOW, Map.of("source", "test"));
    }

    // ---------------------------------------------------------------- constructor

    @Test
    void constructorRejectsNullChannel() {
        assertThrows(IllegalArgumentException.class, () -> new ResilientAuditSink(null, 10));
    }

    @Test
    void constructorRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ResilientAuditSink(channel, 0));
        assertThrows(IllegalArgumentException.class, () -> new ResilientAuditSink(channel, -1));
    }

    // ---------------------------------------------------------------- record()

    @Test
    void recordPersistsUpstreamWhenChannelIsHealthy() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        final AuditEvent e = event("evt-1", "rider-1");

        assertTrue(sink.record(e));

        verify(channel, times(1)).persist(e);
        verifyNoMoreInteractions(channel);
        assertEquals(1, sink.persistedCount());
        assertEquals(0, sink.bufferedCount());
        assertEquals(0, sink.droppedCount());
    }

    @Test
    void recordBuffersEventWhenUpstreamThrows() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        final AuditEvent e = event("evt-1", "rider-1");
        doThrow(new DownstreamUnavailableException("partition")).when(channel).persist(e);

        assertTrue(sink.record(e), "a buffered event still counts as accepted");

        verify(channel, times(1)).persist(e);
        assertEquals(0, sink.persistedCount());
        assertEquals(1, sink.bufferedCount());
        assertTrue(sink.findBuffered("evt-1").isPresent());
    }

    @Test
    void recordDropsEventWhenBufferIsFull() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 1);
        doThrow(new DownstreamUnavailableException("partition")).when(channel).persist(any());

        assertTrue(sink.record(event("evt-1", "rider-1")));
        assertFalse(sink.record(event("evt-2", "rider-1")), "overflow must return false, not throw");

        verify(channel, times(2)).persist(any());
        assertEquals(1, sink.bufferedCount());
        assertEquals(1, sink.droppedCount());
        assertTrue(sink.findBuffered("evt-2").isEmpty());
    }

    @Test
    void recordReturnsFalseForNullEventWithoutTouchingChannel() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);

        assertFalse(sink.record(null));

        verifyNoInteractions(channel);
    }

    @Test
    void recordRejectsEventsAfterClose() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        sink.close();

        assertTrue(sink.isClosed());
        assertFalse(sink.record(event("evt-1", "rider-1")));

        verifyNoInteractions(channel);
        assertEquals(1, sink.droppedCount());
    }

    // ---------------------------------------------------------------- readHistory()

    @Test
    void readHistoryReturnsEmptyForBlankActorWithoutTouchingChannel() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);

        assertTrue(sink.readHistory(null).isEmpty());
        assertTrue(sink.readHistory("   ").isEmpty());

        verifyNoInteractions(channel);
    }

    @Test
    void readHistoryReturnsRemoteResultWhenUpstreamIsHealthy() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        final List<AuditEvent> remote = List.of(event("evt-1", "rider-1"));
        when(channel.read("rider-1")).thenReturn(remote);

        assertEquals(remote, sink.readHistory("rider-1"));

        verify(channel, times(1)).read("rider-1");
    }

    @Test
    void readHistoryFallsBackToLocalBufferWhenUpstreamThrows() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        doThrow(new DownstreamUnavailableException("partition")).when(channel).persist(any());
        sink.record(event("evt-1", "rider-1"));
        sink.record(event("evt-2", "rider-2"));
        when(channel.read("rider-1")).thenThrow(new DownstreamUnavailableException("partition"));

        final List<AuditEvent> history = sink.readHistory("rider-1");

        assertEquals(1, history.size());
        assertEquals("evt-1", history.get(0).getId());
        verify(channel, times(1)).read("rider-1");
    }

    @Test
    void readHistoryFallsBackToLocalBufferWhenUpstreamReturnsNull() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        doThrow(new DownstreamUnavailableException("partition")).when(channel).persist(any());
        sink.record(event("evt-1", "rider-1"));
        when(channel.read("rider-1")).thenReturn(null);

        final List<AuditEvent> history = sink.readHistory("rider-1");

        assertEquals(1, history.size());
        assertEquals("evt-1", history.get(0).getId());
    }

    // ---------------------------------------------------------------- drain()

    @Test
    void drainReplaysBufferedEventsOnceUpstreamRecovers() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        final AuditEvent e = event("evt-1", "rider-1");
        // First call (from record) fails, second call (from drain) succeeds.
        doThrow(new DownstreamUnavailableException("partition")).doNothing().when(channel).persist(e);
        sink.record(e);

        assertEquals(1, sink.drain());

        verify(channel, times(2)).persist(e);
        assertEquals(0, sink.bufferedCount());
    }

    @Test
    void drainStopsAtFirstFailureToPreserveOrdering() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        final AuditEvent first = event("evt-1", "rider-1");
        final AuditEvent second = event("evt-2", "rider-1");
        doThrow(new DownstreamUnavailableException("partition")).when(channel).persist(any());
        sink.record(first);
        sink.record(second);

        assertEquals(0, sink.drain());

        // first: once from record() + once from drain(); second: only from record(),
        // because drain() must stop as soon as the head of the buffer fails.
        verify(channel, times(2)).persist(first);
        verify(channel, times(1)).persist(second);
        assertEquals(2, sink.bufferedCount());
    }

    @Test
    void drainReplaysInInsertionOrder() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);
        final AuditEvent first = event("evt-1", "rider-1");
        final AuditEvent second = event("evt-2", "rider-1");
        doThrow(new DownstreamUnavailableException("partition")).doNothing().when(channel).persist(first);
        doThrow(new DownstreamUnavailableException("partition")).doNothing().when(channel).persist(second);
        sink.record(first);
        sink.record(second);

        assertEquals(2, sink.drain());

        final InOrder order = inOrder(channel);
        order.verify(channel).persist(first);   // record
        order.verify(channel).persist(second);  // record
        order.verify(channel).persist(first);   // drain
        order.verify(channel).persist(second);  // drain
        assertEquals(0, sink.bufferedCount());
    }

    @Test
    void drainOnEmptyBufferDoesNothing() {
        final ResilientAuditSink sink = new ResilientAuditSink(channel, 10);

        assertEquals(0, sink.drain());

        verify(channel, never()).persist(any());
    }
}
