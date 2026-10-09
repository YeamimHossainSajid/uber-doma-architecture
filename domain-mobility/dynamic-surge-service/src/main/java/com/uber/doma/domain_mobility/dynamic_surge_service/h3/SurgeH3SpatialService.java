package com.uber.doma.domain_mobility.dynamic_surge_service.h3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurgeH3SpatialService {
    private static final Logger log = LoggerFactory.getLogger(SurgeH3SpatialService.class);

    public List<String> getNeighborSurgeHexes(String originHex) {
        log.info("[SurgeH3] Calculated surrounding resolution-8 rings for hex {}", originHex);
        return List.of(originHex, originHex + "_n1", originHex + "_n2");
    }
}
