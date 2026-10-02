package com.uber.doma.domain_mobility.supply_locator_service.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DistributedIdempotencyValidator {
    private static final Logger log = LoggerFactory.getLogger(DistributedIdempotencyValidator.class);
    private static final String IDEMPOTENCY_PREFIX = "idemp:supply:";

    private final StringRedisTemplate redisTemplate;

    public DistributedIdempotencyValidator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String idempotencyKey, Duration ttl) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", ttl);
        boolean isUnique = Boolean.TRUE.equals(acquired);
        if (!isUnique) {
            log.warn("[Idempotency] Duplicate request detected for key {}", idempotencyKey);
        }
        return isUnique;
    }
}
