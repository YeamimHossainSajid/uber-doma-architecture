package com.uber.doma.domain_platform.service_registry_discovery_service.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAndDependencyHealthIndicator implements HealthIndicator {

    @Autowired
    private JdbcTemplate template;

    @Override
    public Health health() {
        try {
            template.queryForObject("SELECT 1", Integer.class);

            return Health.up().build();
        } catch (DataAccessException e) {
            return Health.down(e).build();
        }
    }
}
