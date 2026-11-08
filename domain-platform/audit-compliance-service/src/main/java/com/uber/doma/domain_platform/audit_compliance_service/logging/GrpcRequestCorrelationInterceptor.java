package com.uber.doma.domain_platform.audit_compliance_service.logging;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * gRPC counterpart of {@link RequestCorrelationMdcFilter}: reads the
 * {@code x-trace-id} and {@code x-rider-id} metadata entries and binds them
 * into the SLF4J MDC.
 *
 * <p>gRPC may run each listener callback on a different executor thread, so
 * the values are bound around every callback and restored afterwards rather
 * than once in {@link #interceptCall}. The resolved trace ID is also returned
 * to the caller in the response headers.</p>
 */
public class GrpcRequestCorrelationInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of(RequestCorrelationMdc.TRACE_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> RIDER_ID_KEY =
            Metadata.Key.of(RequestCorrelationMdc.RIDER_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
                                                      final Metadata headers,
                                                      final ServerCallHandler<ReqT, RespT> next) {
        final String traceId = RequestCorrelationMdc.resolveTraceId(headers.get(TRACE_ID_KEY));
        final String riderId = RequestCorrelationMdc.sanitize(headers.get(RIDER_ID_KEY));

        final ServerCall<ReqT, RespT> echoingCall =
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void sendHeaders(final Metadata responseHeaders) {
                        responseHeaders.put(TRACE_ID_KEY, traceId);
                        super.sendHeaders(responseHeaders);
                    }
                };

        final RequestCorrelationMdc.Snapshot previous = RequestCorrelationMdc.bind(traceId, riderId);
        try {
            return new MdcBindingListener<>(next.startCall(echoingCall, headers), traceId, riderId);
        } finally {
            RequestCorrelationMdc.restore(previous);
        }
    }

    private static final class MdcBindingListener<ReqT> extends Listener<ReqT> {

        private final Listener<ReqT> delegate;
        private final String traceId;
        private final String riderId;

        MdcBindingListener(final Listener<ReqT> delegate, final String traceId, final String riderId) {
            this.delegate = delegate;
            this.traceId = traceId;
            this.riderId = riderId;
        }

        @Override
        public void onMessage(final ReqT message) {
            run(() -> delegate.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            run(delegate::onHalfClose);
        }

        @Override
        public void onCancel() {
            run(delegate::onCancel);
        }

        @Override
        public void onComplete() {
            run(delegate::onComplete);
        }

        @Override
        public void onReady() {
            run(delegate::onReady);
        }

        private void run(final Runnable callback) {
            final RequestCorrelationMdc.Snapshot previous = RequestCorrelationMdc.bind(traceId, riderId);
            try {
                callback.run();
            } finally {
                RequestCorrelationMdc.restore(previous);
            }
        }
    }
}
