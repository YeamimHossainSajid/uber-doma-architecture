package com.uber.doma.domain_platform.audit_compliance_service.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.ErrorInfo;
import com.uber.doma.domain_platform.audit_compliancREASON_ENTITY_NOT_FOUNDe_service.resilience.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GrpcExceptionTranslator}: each handler is invoked
 * directly and the resulting status code, description and rich
 * {@link ErrorInfo} details are verified.
 */
class GrpcExceptionTranslatorTest {

    private final GrpcExceptionTranslator translator = new GrpcExceptionTranslator();

    @Test
    void entityNotFoundMapsToNotFound() throws Exception {
        final StatusRuntimeException result =
                translator.handleEntityNotFound(new EntityNotFoundException("audit record 42 not found"));

        assertEquals(Status.Code.NOT_FOUND, result.getStatus().getCode());
        assertEquals("audit record 42 not found", result.getStatus().getDescription());
        assertErrorInfo(result, Status.Code.NOT_FOUND,
                GrpcExceptionTranslator.REASON_ENTITY_NOT_FOUND, "EntityNotFoundException");
    }

    @Test
    void illegalArgumentMapsToInvalidArgument() throws Exception {
        final StatusRuntimeException result =
                translator.handleIllegalArgument(new IllegalArgumentException("actorId must not be blank"));

        assertEquals(Status.Code.INVALID_ARGUMENT, result.getStatus().getCode());
        assertEquals("actorId must not be blank", result.getStatus().getDescription());
        assertErrorInfo(result, Status.Code.INVALID_ARGUMENT,
                GrpcExceptionTranslator.REASON_INVALID_ARGUMENT, "IllegalArgumentException");
    }

    @Test
    void illegalArgumentSubclassKeepsItsOwnNameInMetadata() throws Exception {
        final StatusRuntimeException result =
                translator.handleIllegalArgument(new NumberFormatException("not a number"));

        assertEquals(Status.Code.INVALID_ARGUMENT, result.getStatus().getCode());
        assertErrorInfo(result, Status.Code.INVALID_ARGUMENT,
                GrpcExceptionTranslator.REASON_INVALID_ARGUMENT, "NumberFormatException");
    }

    @Test
    void downstreamUnavailableMapsToUnavailable() throws Exception {
        final StatusRuntimeException result = translator.handleDownstreamUnavailable(
                new DownstreamUnavailableException("regulatory-reporting returned 503"));

        assertEquals(Status.Code.UNAVAILABLE, result.getStatus().getCode());
        assertEquals("regulatory-reporting returned 503", result.getStatus().getDescription());
        assertErrorInfo(result, Status.Code.UNAVAILABLE,
                GrpcExceptionTranslator.REASON_DOWNSTREAM_UNAVAILABLE, "DownstreamUnavailableException");
    }

    @Test
    void openCircuitBreakerMapsToUnavailable() throws Exception {
        final CircuitBreaker breaker = CircuitBreaker.ofDefaults("audit-test");
        breaker.transitionToOpenState();

        final StatusRuntimeException result = translator.handleCircuitOpen(
                CallNotPermittedException.createCallNotPermittedException(breaker));

        assertEquals(Status.Code.UNAVAILABLE, result.getStatus().getCode());
        assertNotNull(result.getStatus().getDescription());
        assertTrue(result.getStatus().getDescription().contains("audit-test"));
        assertErrorInfo(result, Status.Code.UNAVAILABLE,
                GrpcExceptionTranslator.REASON_CIRCUIT_OPEN, "CallNotPermittedException");
    }

    @Test
    void missingMessageFallsBackToDefaultDescription() {
        final StatusRuntimeException result =
                translator.handleEntityNotFound(new EntityNotFoundException());

        assertEquals("Requested entity was not found", result.getStatus().getDescription());
    }

    @Test
    void unexpectedExceptionMapsToInternalWithoutLeakingMessage() throws Exception {
        final StatusRuntimeException result = translator.handleUnexpected(
                new IllegalStateException("db password=secret rejected"));

        assertEquals(Status.Code.INTERNAL, result.getStatus().getCode());
        assertEquals(GrpcExceptionTranslator.INTERNAL_DESCRIPTION, result.getStatus().getDescription());
        assertErrorInfo(result, Status.Code.INTERNAL,
                GrpcExceptionTranslator.REASON_INTERNAL, "IllegalStateException");
    }

    @Test
    void statusRuntimeExceptionPassesThroughUnchanged() {
        final StatusRuntimeException original =
                Status.PERMISSION_DENIED.withDescription("GDPR erasure locked").asRuntimeException();

        assertSame(original, translator.handleStatusRuntimeException(original));
    }

    @Test
    void statusExceptionPassesThroughUnchanged() {
        final StatusException original = Status.ALREADY_EXISTS.asException(new Metadata());

        assertSame(original, translator.handleStatusException(original));
    }

    private static void assertErrorInfo(final StatusRuntimeException result,
                                        final Status.Code expectedCode,
                                        final String expectedReason,
                                        final String expectedExceptionName)
            throws InvalidProtocolBufferException {
        final com.google.rpc.Status rpcStatus = StatusProto.fromThrowable(result);
        assertNotNull(rpcStatus, "rich error details must be attached to the trailers");
        assertEquals(expectedCode.value(), rpcStatus.getCode());
        assertEquals(1, rpcStatus.getDetailsCount());

        final Any detail = rpcStatus.getDetails(0);
        assertTrue(detail.is(ErrorInfo.class));
        final ErrorInfo errorInfo = detail.unpack(ErrorInfo.class);
        assertEquals(expectedReason, errorInfo.getReason());
        assertEquals(GrpcExceptionTranslator.ERROR_DOMAIN, errorInfo.getDomain());
        assertEquals(expectedExceptionName,
                errorInfo.getMetadataOrThrow(GrpcExceptionTranslator.METADATA_EXCEPTION));
    }
}
