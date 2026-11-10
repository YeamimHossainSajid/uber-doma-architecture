package com.uber.doma.domain_platform.audit_compliance_service.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the real {@code logback-spring.xml} through Spring Boot's logging
 * system and checks what actually reaches stdout.
 */
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingConfigurationTest {

    private final LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(getClass().getClassLoader());

    @AfterEach
    void resetLogging() throws Exception {
        MDC.clear();
        loggingSystem.cleanUp();
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        new ContextInitializer(context).autoConfig();
    }

    @Test
    void emitsOneJsonObjectPerLineWithCorrelationFields(final CapturedOutput output) throws Exception {
        initialize(new MockEnvironment().withProperty("spring.application.name", "audit-compliance-service"));
        final String message = "audit record persisted " + UUID.randomUUID();

        RequestCorrelationMdc.bind("trace-xyz", "rider-42");
        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        LoggerFactory.getLogger("audit.test").info(message);

        final JsonNode json = new ObjectMapper().readTree(lineContaining(output, message));
        assertThat(json.path("message").asText()).isEqualTo(message);
        assertThat(json.path("level").asText()).isEqualTo("INFO");
        assertThat(json.path("logger_name").asText()).isEqualTo("audit.test");
        assertThat(json.path("@timestamp").asText()).isNotBlank();
        assertThat(json.path("service").asText()).isEqualTo("audit-compliance-service");
        assertThat(json.path(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID).asText()).isEqualTo("trace-xyz");
        assertThat(json.path(RequestCorrelationMdc.MDC_RIDER_ID).asText()).isEqualTo("rider-42");
        assertThat(json.path("traceId").asText()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void localProfileUsesReadablePattern(final CapturedOutput output) {
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        initialize(environment);
        final String message = "local dev line " + UUID.randomUUID();

        RequestCorrelationMdc.bind("trace-xyz", "rider-42");
        LoggerFactory.getLogger("audit.test").info(message);

        final String line = lineContaining(output, message);
        assertThat(line).doesNotStartWith("{");
        assertThat(line).contains("requestTraceId=trace-xyz", "riderId=rider-42", " INFO ");
    }

    private void initialize(final MockEnvironment environment) {
        // A @SpringBootTest earlier in the same JVM leaves the shared LoggerContext
        // marked as initialized, which would make initialize() a silent no-op.
        loggingSystem.cleanUp();
        loggingSystem.beforeInitialize();
        loggingSystem.initialize(new LoggingInitializationContext(environment), "classpath:logback-spring.xml", null);
    }

    private static String lineContaining(final CapturedOutput output, final String marker) {
        return Arrays.stream(output.getOut().split("\\R"))
                .filter(line -> line.contains(marker))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no log line containing: " + marker + "\n" + output.getOut()));
    }
}
