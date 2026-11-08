package com.uber.doma.domain_platform.audit_compliance_service.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that binds {@code X-Trace-ID} and {@code X-Rider-ID} into the
 * SLF4J MDC for the duration of an HTTP request and echoes the resolved trace
 * ID back on the response so callers can quote it when reporting problems.
 */
public class RequestCorrelationMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        final String traceId = RequestCorrelationMdc.resolveTraceId(
                request.getHeader(RequestCorrelationMdc.TRACE_ID_HEADER));
        final String riderId = RequestCorrelationMdc.sanitize(
                request.getHeader(RequestCorrelationMdc.RIDER_ID_HEADER));

        response.setHeader(RequestCorrelationMdc.TRACE_ID_HEADER, traceId);

        final RequestCorrelationMdc.Snapshot previous = RequestCorrelationMdc.bind(traceId, riderId);
        try {
            chain.doFilter(request, response);
        } finally {
            RequestCorrelationMdc.restore(previous);
        }
    }
}
