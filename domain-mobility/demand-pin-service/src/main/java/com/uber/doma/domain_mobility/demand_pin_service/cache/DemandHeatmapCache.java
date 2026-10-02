package com.uber.doma.domain_mobility.demand_pin_service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DemandHeatmapCache {
    private static final Logger log = LoggerFactory.getLogger(DemandHeatmapCache.class);
    private static final String HEATMAP_KEY = "mobility:demand:heatmap:";

    private final StringRedisTemplate redisTemplate;

    public DemandHeatmapCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheHexDemand(String hexId, int count) {
        redisTemplate.opsForValue().set(HEATMAP_KEY + hexId, String.valueOf(count), Duration.ofSeconds(60));
        log.info("[DemandCache] Cached demand count {} for hex {} (TTL 60s)", count, hexId);
    }
}
