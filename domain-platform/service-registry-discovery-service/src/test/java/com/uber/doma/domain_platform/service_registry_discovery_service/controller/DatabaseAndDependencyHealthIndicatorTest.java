package com.uber.doma.domain_platform.service_registry_discovery_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.doma.domain_platform.service_registry_discovery_service.health.DatabaseAndDependencyHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest("management.endpoint.health.show-details=always")
@AutoConfigureMockMvc
public class DatabaseAndDependencyHealthIndicatorTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseAndDependencyHealthIndicator healthIndicator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void checksDatabaseAndDependencies() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.databaseAndDependency.status").value("UP"))
//                .andDo(print())
                .andExpect(status().isOk());

    }
}
