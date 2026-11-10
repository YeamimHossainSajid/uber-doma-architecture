package com.uber.doma.domain_platform.audit_compliance_service.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestCorrelationMdcFilterTest {

    private final RequestCorrelationMdcFilter filter = new RequestCorrelationMdcFilter();
    private final Map<String, String> seenInChain = new HashMap<>();
    private final FilterChain capturingChain = (req, res) -> {
        seenInChain.put("trace", MDC.get(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID));
        seenInChain.put("rider", MDC.get(RequestCorrelationMdc.MDC_RIDER_ID));
    };

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void bindsHeadersIntoMdcDuringRequestAndClearsAfter() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(RequestCorrelationMdc.TRACE_ID_HEADER, "trace-abc");
        request.addHeader(RequestCorrelationMdc.RIDER_ID_HEADER, "rider-42");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, capturingChain);

        assertThat(seenInChain).containsEntry("trace", "trace-abc").containsEntry("rider", "rider-42");
        assertThat(response.getHeader(RequestCorrelationMdc.TRACE_ID_HEADER)).isEqualTo("trace-abc");
        assertThat(MDC.get(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID)).isNull();
        assertThat(MDC.get(RequestCorrelationMdc.MDC_RIDER_ID)).isNull();
    }

    @Test
    void generatesTraceIdWhenHeaderMissingAndOmitsRider() throws Exception {
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/"), response, capturingChain);

        assertThat(seenInChain.get("trace")).matches("[0-9a-f]{32}");
        assertThat(seenInChain.get("rider")).isNull();
        assertThat(response.getHeader(RequestCorrelationMdc.TRACE_ID_HEADER)).isEqualTo(seenInChain.get("trace"));
    }

    @Test
    void discardsUnsafeHeaderValues() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(RequestCorrelationMdc.TRACE_ID_HEADER, "evil\nINFO forged entry");
        request.addHeader(RequestCorrelationMdc.RIDER_ID_HEADER, "rider 42");

        filter.doFilter(request, new MockHttpServletResponse(), capturingChain);

        assertThat(seenInChain.get("trace")).matches("[0-9a-f]{32}");
        assertThat(seenInChain.get("rider")).isNull();
    }

    @Test
    void restoresMdcEvenWhenChainThrows() {
        MDC.put(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID, "outer");
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(RequestCorrelationMdc.TRACE_ID_HEADER, "inner");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            throw new IllegalStateException("boom");
        })).hasMessage("boom");

        assertThat(MDC.get(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID)).isEqualTo("outer");
    }
}
