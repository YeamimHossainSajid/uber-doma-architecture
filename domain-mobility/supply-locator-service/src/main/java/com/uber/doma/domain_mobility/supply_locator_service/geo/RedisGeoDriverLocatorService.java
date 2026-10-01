package com.uber.doma.domain_mobility.supply_locator_service.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class RedisGeoDriverLocatorService {
    private static final Logger log = LoggerFactory.getLogger(RedisGeoDriverLocatorService.class);
    private static final String GEO_KEY_PREFIX = "mobility:driver:geo:";

    private final StringRedisTemplate redisTemplate;

    public RedisGeoDriverLocatorService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void indexDriverLocation(String vehicleClass, DriverLocation location) {
        String key = GEO_KEY_PREFIX + vehicleClass;
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        geoOps.add(key, new Point(location.longitude(), location.latitude()), location.driverId());
        log.info("[SupplyLocator] Indexed driver {} at ({}, {}) in {}", 
            location.driverId(), location.latitude(), location.longitude(), key);
    }

    public List<String> findNearbyDrivers(String vehicleClass, double lat, double lng, double radiusKm, int limit) {
        String key = GEO_KEY_PREFIX + vehicleClass;
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();

        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
            .includeDistance()
            .sortAscending()
            .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOps.radius(key, circle, args);
        List<String> nearbyDriverIds = new ArrayList<>();
        if (results != null) {
            results.forEach(result -> nearbyDriverIds.add(result.getContent().getName()));
        }
        return nearbyDriverIds;
    }
}
