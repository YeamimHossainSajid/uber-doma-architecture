package com.uber.doma.domain_platform.audit_compliance_service.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Mockito-based unit tests for {@link ReactiveAuditQueryAggregator}. Both
 * gRPC client ports are mocked so each fan-out branch (success, error,
 * timeout) can be driven independently and the interactions verified.
 */
@ExtendWith(MockitoExtension.class)
class ReactiveAuditQueryAggregatorMockitoTest {

    private static final Duration TIMEOUT = Duration.ofMillis(100);
    private static final AuditQuery QUERY = new AuditQuery(
            "rider-1", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"));

    private static final AuditStorageView AUDIT_VIEW =
            new AuditStorageView("rider-1", List.of("evt-1", "evt-2"), false);
    private static final RegulatoryReportView REPORT_VIEW =
            new RegulatoryReportView("rider-1", List.of("finding-1"), false);

    @Mock
    private ReactiveAuditStorageGrpcClient auditClient;

    @Mock
    private ReactiveRegulatoryReportGrpcClient reportClient;

    private ReactiveAuditQueryAggregator aggregator() {
        return new ReactiveAuditQueryAggregator(auditClient, reportClient, TIMEOUT);
    }

    // ---------------------------------------------------------------- constructor

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(NullPointerException.class,
                () -> new ReactiveAuditQueryAggregator(null, reportClient, TIMEOUT));
        assertThrows(NullPointerException.class,
                () -> new ReactiveAuditQueryAggregator(auditClient, null, TIMEOUT));
        assertThrows(NullPointerException.class,
                () -> new ReactiveAuditQueryAggregator(auditClient, reportClient, null));
    }

    @Test
    void constructorRejectsNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReactiveAuditQueryAggregator(auditClient, reportClient, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new ReactiveAuditQueryAggregator(auditClient, reportClient, Duration.ofMillis(-1)));
    }

    // ---------------------------------------------------------------- aggregate()

    @Test
    void aggregateRejectsNullQueryWithoutCallingClients() {
        final ReactiveAuditQueryAggregator aggregator = aggregator();

        assertThrows(NullPointerException.class, () -> aggregator.aggregate(null));

        verifyNoInteractions(auditClient, reportClient);
    }

    @Test
    void aggregateCombinesBothViewsWhenBothClientsSucceed() {
        when(auditClient.query(QUERY)).thenReturn(Mono.just(AUDIT_VIEW));
        when(reportClient.query(QUERY)).thenReturn(Mono.just(REPORT_VIEW));

        StepVerifier.create(aggregator().aggregate(QUERY))
                .assertNext(result -> {
                    assertEquals(AUDIT_VIEW, result.auditTrail());
                    assertEquals(REPORT_VIEW, result.regulatoryReport());
                    assertFalse(result.degraded());
                })
                .verifyComplete();

        verify(auditClient, times(1)).query(QUERY);
        verify(reportClient, times(1)).query(QUERY);
    }

    @Test
    void aggregateFallsBackOnlyForAuditStorageWhenItErrors() {
        when(auditClient.query(QUERY)).thenReturn(Mono.error(new RuntimeException("audit-storage 503")));
        when(reportClient.query(QUERY)).thenReturn(Mono.just(REPORT_VIEW));

        StepVerifier.create(aggregator().aggregate(QUERY))
                .assertNext(result -> {
                    assertEquals(AuditStorageView.fallback("rider-1"), result.auditTrail());
                    assertEquals(REPORT_VIEW, result.regulatoryReport());
                    assertTrue(result.degraded());
                })
                .verifyComplete();
    }

    @Test
    void aggregateFallsBackOnlyForRegulatoryReportWhenItErrors() {
        when(auditClient.query(QUERY)).thenReturn(Mono.just(AUDIT_VIEW));
        when(reportClient.query(QUERY)).thenReturn(Mono.error(new RuntimeException("reporting 500")));

        StepVerifier.create(aggregator().aggregate(QUERY))
                .assertNext(result -> {
                    assertEquals(AUDIT_VIEW, result.auditTrail());
                    assertEquals(RegulatoryReportView.fallback("rider-1"), result.regulatoryReport());
                    assertTrue(result.degraded());
                })
                .verifyComplete();
    }

    @Test
    void aggregateFallsBackForBothWhenBothClientsError() {
        when(auditClient.query(QUERY)).thenReturn(Mono.error(new RuntimeException("down")));
        when(reportClient.query(QUERY)).thenReturn(Mono.error(new RuntimeException("down")));

        StepVerifier.create(aggregator().aggregate(QUERY))
                .assertNext(result -> {
                    assertTrue(result.auditTrail().fallback());
                    assertTrue(result.regulatoryReport().fallback());
                    assertTrue(result.degraded());
                })
                .verifyComplete();
    }

    @Test
    void aggregateFallsBackWhenAClientNeverRespondsWithinTimeout() {
        when(auditClient.query(QUERY)).thenReturn(Mono.never());
        when(reportClient.query(QUERY)).thenReturn(Mono.just(REPORT_VIEW));

        // Virtual time: the timeout elapses instantly instead of slowing the test suite.
        StepVerifier.withVirtualTime(() -> aggregator().aggregate(QUERY))
                .thenAwait(TIMEOUT)
                .assertNext(result -> {
                    assertTrue(result.auditTrail().fallback());
                    assertEquals(REPORT_VIEW, result.regulatoryReport());
                })
                .verifyComplete();
    }
}
