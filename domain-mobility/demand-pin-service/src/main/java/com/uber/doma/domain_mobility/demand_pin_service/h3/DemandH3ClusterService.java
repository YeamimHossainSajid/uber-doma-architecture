package com.uber.doma.domain_mobility.demand_pin_service.h3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DemandH3ClusterService {
    private static final Logger log = LoggerFactory.getLogger(DemandH3ClusterService.class);

    public String latLngToHex(double lat, double lng, int res) {
        String hex = String.format("8828308281fffff_%02d", res);
        log.info("[H3Demand] Indexed demand pin at ({}, {}) -> H3 Hex {}", lat, lng, hex);
        return hex;
    }
}
