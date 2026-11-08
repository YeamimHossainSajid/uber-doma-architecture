package com.uber.doma.domain_platform.audit_compliance_service.config;

import com.uber.doma.domain_platform.audit_compliance_service.logging.GrpcRequestCorrelationInterceptor;
import com.uber.doma.domain_platform.audit_compliance_service.logging.RequestCorrelationMdcFilter;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the HTTP and gRPC entry points that bind {@code X-Trace-ID} and
 * {@code X-Rider-ID} into the SLF4J MDC for structured logging.
 */
@Configuration
public class LoggingConfiguration {

    /**
     * Runs just after Spring Boot's {@code ServerHttpObservationFilter}
     * ({@code HIGHEST_PRECEDENCE + 1}) so the request span is already active
     * when a trace ID has to be derived from it.
     */
    public static final int CORRELATION_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

    @Bean
    public FilterRegistrationBean<RequestCorrelationMdcFilter> requestCorrelationMdcFilter() {
        final FilterRegistrationBean<RequestCorrelationMdcFilter> registration =
                new FilterRegistrationBean<>(new RequestCorrelationMdcFilter());
        registration.setOrder(CORRELATION_FILTER_ORDER);
        return registration;
    }

    @Bean
    @GrpcGlobalServerInterceptor
    public GrpcRequestCorrelationInterceptor grpcRequestCorrelationInterceptor() {
        return new GrpcRequestCorrelationInterceptor();
    }
}
