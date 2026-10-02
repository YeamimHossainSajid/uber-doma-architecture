package com.uber.doma.domain_mobility.supply_locator_service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DriverLocationCacheService {
    private static final Logger log = LoggerFactory.getLogger(DriverLocationCacheService.class);
    private static final String CACHE_PREFIX = "cache:driver:loc:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public DriverLocationCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheDriverLocation(String driverId, String geoPayload) {
        redisTemplate.opsForValue().set(CACHE_PREFIX + driverId, geoPayload, DEFAULT_TTL);
        log.info("[SupplyLocatorCache] Cached location for driver {} with TTL 5m", driverId);
    }

    public String getCachedLocation(String driverId) {
        return redisTemplate.opsForValue().get(CACHE_PREFIX + driverId);
    }

    public void evictDriverLocation(String driverId) {
        redisTemplate.delete(CACHE_PREFIX + driverId);
    }
}
