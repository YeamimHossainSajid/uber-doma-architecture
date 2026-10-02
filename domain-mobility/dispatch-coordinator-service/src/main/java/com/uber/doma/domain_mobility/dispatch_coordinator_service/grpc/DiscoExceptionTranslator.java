package com.uber.doma.domain_mobility.dispatch_coordinator_service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;

@Component
public class DiscoExceptionTranslator {

    public StatusRuntimeException translate(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(t.getMessage()).asRuntimeException();
        }
        return Status.INTERNAL.withDescription("Internal error in DISCO matching engine").asRuntimeException();
    }
}
