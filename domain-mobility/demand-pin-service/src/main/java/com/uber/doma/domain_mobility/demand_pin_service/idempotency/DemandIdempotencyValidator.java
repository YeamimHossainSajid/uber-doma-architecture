package com.uber.doma.domain_mobility.demand_pin_service.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DemandIdempotencyValidator {
    private static final Logger log = LoggerFactory.getLogger(DemandIdempotencyValidator.class);
    private static final String PREFIX = "idemp:demand:";

    private final StringRedisTemplate redisTemplate;

    public DemandIdempotencyValidator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean validate(String idempotencyKey) {
        Boolean set = redisTemplate.opsForValue().setIfAbsent(PREFIX + idempotencyKey, "OK", Duration.ofSeconds(120));
        boolean valid = Boolean.TRUE.equals(set);
        if (!valid) {
            log.warn("[DemandIdemp] Suppressed duplicate demand pin submission for key {}", idempotencyKey);
        }
        return valid;
    }
}
