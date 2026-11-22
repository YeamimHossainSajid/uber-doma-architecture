package com.uber.doma.domain_platform.audit_compliance_service.grpc;

import com.google.rpc.ErrorInfo;
import com.uber.doma.domain_platform.audit_compliance_service.resilience.DownstreamUnavailableException;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import jakarta.persistence.EntityNotFoundException;
import net.devh.boot.grpc.common.autoconfigure.GrpcCommonCodecAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration;
import net.devh.boot.grpc.server.service.GrpcService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * End-to-end test of {@link GrpcExceptionTranslator} through the real
 * grpc-spring {@code @GrpcAdvice} pipeline: a test {@link GrpcService}
 * throws exceptions, and an in-process client asserts on the status code
 * and the rich {@link ErrorInfo} details received over the wire.
 */
@SpringBootTest(
        classes = GrpcExceptionTranslatorIntegrationTest.TestConfig.class,
        properties = {
                "grpc.server.inProcessName=" + GrpcExceptionTranslatorIntegrationTest.IN_PROCESS_NAME,
                "grpc.server.port=-1"
        })
@DirtiesContext
class GrpcExceptionTranslatorIntegrationTest {

    static final String IN_PROCESS_NAME = "audit-compliance-exception-translator-test";

    private static final MethodDescriptor.Marshaller<String> STRING_MARSHALLER =
            new MethodDescriptor.Marshaller<>() {
                @Override
                public InputStream stream(final String value) {
                    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
                }

                @Override
                public String parse(final InputStream stream) {
                    try {
                        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (final IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }
            };

    private static final MethodDescriptor<String, String> METHOD = MethodDescriptor.<String, String>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("audit.ExceptionProbe/Call")
            .setRequestMarshaller(STRING_MARSHALLER)
            .setResponseMarshaller(STRING_MARSHALLER)
            .build();

    private ManagedChannel channel;

    @BeforeEach
    void openChannel() {
        channel = InProcessChannelBuilder.forName(IN_PROCESS_NAME).directExecutor().build();
    }

    @AfterEach
    void closeChannel() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void successfulCallIsUnaffected() {
        assertEquals("ok", call("ok"));
    }

    @Test
    void entityNotFoundIsReceivedAsNotFound() throws Exception {
        final StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () -> call("not-found"));

        assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
        assertEquals("audit record missing", ex.getStatus().getDescription());
        assertEquals(GrpcExceptionTranslator.REASON_ENTITY_NOT_FOUND, errorInfo(ex).getReason());
    }

    @Test
    void illegalArgumentIsReceivedAsInvalidArgument() throws Exception {
        final StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () -> call("bad-arg"));

        assertEquals(Status.Code.INVALID_ARGUMENT, ex.getStatus().getCode());
        assertEquals("actorId must not be blank", ex.getStatus().getDescription());
        assertEquals(GrpcExceptionTranslator.REASON_INVALID_ARGUMENT, errorInfo(ex).getReason());
    }

    @Test
    void downstreamUnavailableIsReceivedAsUnavailable() throws Exception {
        final StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () -> call("downstream"));

        assertEquals(Status.Code.UNAVAILABLE, ex.getStatus().getCode());
        assertEquals(GrpcExceptionTranslator.REASON_DOWNSTREAM_UNAVAILABLE, errorInfo(ex).getReason());
    }

    @Test
    void unexpectedExceptionIsReceivedAsInternalWithGenericDescription() throws Exception {
        final StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () -> call("boom"));

        assertEquals(Status.Code.INTERNAL, ex.getStatus().getCode());
        assertEquals(GrpcExceptionTranslator.INTERNAL_DESCRIPTION, ex.getStatus().getDescription());
        assertEquals(GrpcExceptionTranslator.REASON_INTERNAL, errorInfo(ex).getReason());
    }

    @Test
    void explicitStatusExceptionIsPassedThrough() {
        final StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () -> call("status"));

        assertEquals(Status.Code.PERMISSION_DENIED, ex.getStatus().getCode());
        assertEquals("GDPR erasure locked", ex.getStatus().getDescription());
    }

    private String call(final String payload) {
        return ClientCalls.blockingUnaryCall(channel, METHOD, io.grpc.CallOptions.DEFAULT, payload);
    }

    private static ErrorInfo errorInfo(final StatusRuntimeException ex) throws Exception {
        final com.google.rpc.Status rpcStatus = StatusProto.fromThrowable(ex);
        assertNotNull(rpcStatus, "rich error details must survive the wire");
        assertEquals(1, rpcStatus.getDetailsCount());
        final ErrorInfo info = rpcStatus.getDetails(0).unpack(ErrorInfo.class);
        assertEquals(GrpcExceptionTranslator.ERROR_DOMAIN, info.getDomain());
        return info;
    }

    /** Test service that throws a different exception depending on the request payload. */
    @GrpcService
    static class ExceptionProbeService implements BindableService {

        @Override
        public ServerServiceDefinition bindService() {
            final ServerCalls.UnaryMethod<String, String> handler = (request, observer) -> {
                switch (request) {
                    case "not-found" -> throw new EntityNotFoundException("audit record missing");
                    case "bad-arg" -> throw new IllegalArgumentException("actorId must not be blank");
                    case "downstream" -> throw new DownstreamUnavailableException("audit storage returned 503");
                    case "boom" -> throw new IllegalStateException("internal detail that must not leak");
                    case "status" -> throw Status.PERMISSION_DENIED
                            .withDescription("GDPR erasure locked").asRuntimeException();
                    default -> {
                        observer.onNext(request);
                        observer.onCompleted();
                    }
                }
            };
            return ServerServiceDefinition.builder("audit.ExceptionProbe")
                    .addMethod(ServerMethodDefinition.create(METHOD, ServerCalls.asyncUnaryCall(handler)))
                    .build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({
            GrpcCommonCodecAutoConfiguration.class,
            GrpcServerAutoConfiguration.class,
            GrpcServerFactoryAutoConfiguration.class,
            GrpcAdviceAutoConfiguration.class
    })
    @Import(GrpcExceptionTranslator.class)
    static class TestConfig {

        @Bean
        ExceptionProbeService exceptionProbeService() {
            return new ExceptionProbeService();
        }
    }
}
