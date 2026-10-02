package com.uber.doma.domain_mobility.dispatch_coordinator_service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DiscoBatchCache {
    private static final Logger log = LoggerFactory.getLogger(DiscoBatchCache.class);
    private static final String BATCH_PREFIX = "mobility:disco:batch:";

    private final StringRedisTemplate redisTemplate;

    public DiscoBatchCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheBatchRound(String batchId, String candidateJson) {
        redisTemplate.opsForValue().set(BATCH_PREFIX + batchId, candidateJson, Duration.ofSeconds(30));
        log.info("[DiscoCache] Cached candidate batch {} with 30s TTL", batchId);
    }
}
