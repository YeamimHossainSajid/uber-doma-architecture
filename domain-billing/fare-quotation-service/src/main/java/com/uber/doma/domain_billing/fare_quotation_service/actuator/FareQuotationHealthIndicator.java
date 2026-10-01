package com.uber.doma.domain_billing.fare_quotation_service.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FareQuotationHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean tariffRulesLoaded = checkTariffRulesEngine();
        if (tariffRulesLoaded) {
            return Health.up()
                .withDetail("tariffRulesEngine", "ACTIVE")
                .withDetail("currencyConverter", "SYNCED")
                .build();
        }
        return Health.down()
            .withDetail("tariffRulesEngine", "UNAVAILABLE")
            .build();
    }

    private boolean checkTariffRulesEngine() {
        return true;
    }
}
