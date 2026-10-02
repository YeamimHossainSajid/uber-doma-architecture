package com.uber.doma.domain_mobility.demand_pin_service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;

@Component
public class DemandPinExceptionTranslator {

    public StatusRuntimeException translate(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(t.getMessage()).asRuntimeException();
        }
        return Status.INTERNAL.withDescription("Internal error in demand pin service").asRuntimeException();
    }
}
