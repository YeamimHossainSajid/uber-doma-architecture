package com.uber.doma.domain_platform.audit_compliance_service.grpc;

import com.google.protobuf.Any;
import com.google.rpc.ErrorInfo;
import com.uber.doma.domain_platform.audit_compliance_service.resilience.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import jakarta.persistence.EntityNotFoundException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global translator that maps Java and domain exceptions thrown by gRPC
 * service implementations in the audit-compliance-service to gRPC
 * {@link StatusRuntimeException}s with the correct status code.
 *
 * <table>
 *     <caption>Exception to status mapping</caption>
 *     <tr><th>Exception</th><th>gRPC status</th><th>{@link ErrorInfo#getReason() reason}</th></tr>
 *     <tr><td>{@link EntityNotFoundException}</td><td>{@code NOT_FOUND}</td><td>{@value #REASON_ENTITY_NOT_FOUND}</td></tr>
 *     <tr><td>{@link IllegalArgumentException}</td><td>{@code INVALID_ARGUMENT}</td><td>{@value #REASON_INVALID_ARGUMENT}</td></tr>
 *     <tr><td>{@link DownstreamUnavailableException}</td><td>{@code UNAVAILABLE}</td><td>{@value #REASON_DOWNSTREAM_UNAVAILABLE}</td></tr>
 *     <tr><td>{@link CallNotPermittedException}</td><td>{@code UNAVAILABLE}</td><td>{@value #REASON_CIRCUIT_OPEN}</td></tr>
 *     <tr><td>{@link StatusRuntimeException} / {@link StatusException}</td><td>unchanged</td><td>unchanged</td></tr>
 *     <tr><td>any other {@link Exception}</td><td>{@code INTERNAL}</td><td>{@value #REASON_INTERNAL}</td></tr>
 * </table>
 *
 * <p>Every translated error carries rich details using the standard
 * <a href="https://cloud.google.com/apis/design/errors#error_model">Google
 * error model</a>: a {@code google.rpc.Status} holding an {@link ErrorInfo}
 * is attached to the response trailers under {@code grpc-status-details-bin}.
 * Clients read it with {@link StatusProto#fromThrowable(Throwable)}.</p>
 *
 * <p>Unexpected exceptions are logged with their stack trace, but the client
 * only receives a generic description so internal details never leak across
 * the service boundary.</p>
 *
 * <p>Note: grpc-spring only routes exceptions that are <em>thrown</em> from a
 * service method to this advice. Errors passed directly to
 * {@code StreamObserver#onError} bypass it.</p>
 */
@GrpcAdvice
public class GrpcExceptionTranslator {

    /** {@link ErrorInfo#getDomain()} value identifying this service as the error source. */
    public static final String ERROR_DOMAIN = "audit-compliance-service";

    /** {@link ErrorInfo#getMetadataMap()} key holding the simple name of the translated exception. */
    public static final String METADATA_EXCEPTION = "exception";

    public static final String REASON_ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";
    public static final String REASON_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String REASON_DOWNSTREAM_UNAVAILABLE = "DOWNSTREAM_UNAVAILABLE";
    public static final String REASON_CIRCUIT_OPEN = "CIRCUIT_OPEN";
    public static final String REASON_INTERNAL = "INTERNAL_ERROR";

    /** Description returned to clients for unexpected failures. */
    static final String INTERNAL_DESCRIPTION = "Internal server error";

    private static final Logger log = LoggerFactory.getLogger(GrpcExceptionTranslator.class);

    @GrpcExceptionHandler(EntityNotFoundException.class)
    public StatusRuntimeException handleEntityNotFound(final EntityNotFoundException ex) {
        log.debug("Translating EntityNotFoundException to NOT_FOUND: {}", ex.getMessage());
        return translate(Status.NOT_FOUND, REASON_ENTITY_NOT_FOUND,
                descriptionOrDefault(ex, "Requested entity was not found"), ex);
    }

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleIllegalArgument(final IllegalArgumentException ex) {
        log.debug("Translating IllegalArgumentException to INVALID_ARGUMENT: {}", ex.getMessage());
        return translate(Status.INVALID_ARGUMENT, REASON_INVALID_ARGUMENT,
                descriptionOrDefault(ex, "Request contained an invalid argument"), ex);
    }

    @GrpcExceptionHandler(DownstreamUnavailableException.class)
    public StatusRuntimeException handleDownstreamUnavailable(final DownstreamUnavailableException ex) {
        log.warn("Translating DownstreamUnavailableException to UNAVAILABLE: {}", ex.getMessage());
        return translate(Status.UNAVAILABLE, REASON_DOWNSTREAM_UNAVAILABLE,
                descriptionOrDefault(ex, "A downstream dependency is unavailable"), ex);
    }

    @GrpcExceptionHandler(CallNotPermittedException.class)
    public StatusRuntimeException handleCircuitOpen(final CallNotPermittedException ex) {
        log.warn("Translating CallNotPermittedException to UNAVAILABLE: {}", ex.getMessage());
        return translate(Status.UNAVAILABLE, REASON_CIRCUIT_OPEN,
                descriptionOrDefault(ex, "Circuit breaker is open"), ex);
    }

    /**
     * Status exceptions already carry a deliberate status (and possibly
     * trailers), so they are passed through unchanged instead of being
     * swallowed by the {@link Exception} fallback below.
     */
    @GrpcExceptionHandler(StatusRuntimeException.class)
    public StatusRuntimeException handleStatusRuntimeException(final StatusRuntimeException ex) {
        return ex;
    }

    @GrpcExceptionHandler(StatusException.class)
    public StatusException handleStatusException(final StatusException ex) {
        return ex;
    }

    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleUnexpected(final Exception ex) {
        log.error("Unhandled exception in gRPC call, translating to INTERNAL", ex);
        return translate(Status.INTERNAL, REASON_INTERNAL, INTERNAL_DESCRIPTION, ex);
    }

    private static StatusRuntimeException translate(final Status status,
                                                    final String reason,
                                                    final String description,
                                                    final Throwable cause) {
        final ErrorInfo errorInfo = ErrorInfo.newBuilder()
                .setReason(reason)
                .setDomain(ERROR_DOMAIN)
                .putMetadata(METADATA_EXCEPTION, cause.getClass().getSimpleName())
                .build();
        final com.google.rpc.Status rpcStatus = com.google.rpc.Status.newBuilder()
                .setCode(status.getCode().value())
                .setMessage(description)
                .addDetails(Any.pack(errorInfo))
                .build();
        return StatusProto.toStatusRuntimeException(rpcStatus);
    }

    private static String descriptionOrDefault(final Throwable ex, final String fallback) {
        final String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
