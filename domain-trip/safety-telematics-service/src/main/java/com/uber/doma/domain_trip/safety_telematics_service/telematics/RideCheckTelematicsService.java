package com.uber.doma.domain_trip.safety_telematics_service.telematics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RideCheckTelematicsService {
    private static final Logger log = LoggerFactory.getLogger(RideCheckTelematicsService.class);
    private static final double CRASH_G_FORCE_THRESHOLD = 4.0;

    public boolean evaluateFrame(AccelerometerFrame frame) {
        double magnitude = Math.sqrt(
            frame.gForceX() * frame.gForceX() +
            frame.gForceY() * frame.gForceY() +
            frame.gForceZ() * frame.gForceZ()
        );

        if (magnitude >= CRASH_G_FORCE_THRESHOLD && frame.currentSpeedMph() > 15.0) {
            log.warn("[RideCheck] 🚨 CRASH ANOMALY DETECTED on Trip {}: Magnitude={}G at {} mph",
                frame.tripId(), String.format("%.2f", magnitude), frame.currentSpeedMph());
            return true;
        }

        return false;
    }
}
