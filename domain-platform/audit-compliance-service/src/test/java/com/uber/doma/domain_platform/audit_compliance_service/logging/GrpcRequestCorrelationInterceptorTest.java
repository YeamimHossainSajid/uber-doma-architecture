package com.uber.doma.domain_platform.audit_compliance_service.logging;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GrpcRequestCorrelationInterceptorTest {

    private final GrpcRequestCorrelationInterceptor interceptor = new GrpcRequestCorrelationInterceptor();
    private final List<String> observed = new ArrayList<>();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void bindsMetadataIntoMdcAroundEveryCallbackAndEchoesTraceId() {
        final Metadata headers = new Metadata();
        headers.put(GrpcRequestCorrelationInterceptor.TRACE_ID_KEY, "grpc-trace-1");
        headers.put(GrpcRequestCorrelationInterceptor.RIDER_ID_KEY, "rider-9");
        final ServerCall<String, String> call = mock(ServerCall.class);

        final ServerCall.Listener<String> listener = interceptor.interceptCall(call, headers, recordingHandler());

        assertThat(MDC.get(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID)).isNull();
        listener.onMessage("payload");
        listener.onHalfClose();
        listener.onComplete();

        assertThat(observed).containsExactly(
                "startCall grpc-trace-1 rider-9",
                "onMessage grpc-trace-1 rider-9",
                "onHalfClose grpc-trace-1 rider-9",
                "onComplete grpc-trace-1 rider-9");
        assertThat(MDC.get(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID)).isNull();
        assertThat(MDC.get(RequestCorrelationMdc.MDC_RIDER_ID)).isNull();

        final ArgumentCaptor<Metadata> sent = ArgumentCaptor.forClass(Metadata.class);
        verify(call).sendHeaders(sent.capture());
        assertThat(sent.getValue().get(GrpcRequestCorrelationInterceptor.TRACE_ID_KEY)).isEqualTo("grpc-trace-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesTraceIdWhenMetadataMissing() {
        final ServerCall<String, String> call = mock(ServerCall.class);

        final ServerCall.Listener<String> listener =
                interceptor.interceptCall(call, new Metadata(), recordingHandler());
        listener.onCancel();

        assertThat(observed).hasSize(2);
        assertThat(observed.get(1)).matches("onCancel [0-9a-f]{32} null");
        assertThat(observed.get(0).split(" ")[1]).isEqualTo(observed.get(1).split(" ")[1]);
    }

    private ServerCallHandler<String, String> recordingHandler() {
        return (call, headers) -> {
            record("startCall");
            call.sendHeaders(new Metadata());
            return new ServerCall.Listener<>() {
                @Override
                public void onMessage(final String message) {
                    record("onMessage");
                }

                @Override
                public void onHalfClose() {
                    record("onHalfClose");
                }

                @Override
                public void onCancel() {
                    record("onCancel");
                }

                @Override
                public void onComplete() {
                    record("onComplete");
                }
            };
        };
    }

    private void record(final String event) {
        observed.add(event + " " + MDC.get(RequestCorrelationMdc.MDC_REQUEST_TRACE_ID)
                + " " + MDC.get(RequestCorrelationMdc.MDC_RIDER_ID));
    }
}
