package com.uber.doma.domain_platform.experimentation_flipr_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.doma.domain_platform.experimentation_flipr_service.dto.EvaluateFlagRequest;
import com.uber.doma.domain_platform.experimentation_flipr_service.dto.GlobalValidationExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExperimentationFliprController.class)
@Import(GlobalValidationExceptionHandler.class)
public class ExperimentationValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EVALUATE_URL = "/api/v1/experiments/evaluate";
    @Test
    @DisplayName("Valid payload returns 200 OK")
    void validPayload_returns200() throws Exception {
        var request = new EvaluateFlagRequest("search_v2_experiment", "user_12345", 50.0, "treatment");
        mockMvc.perform(post(EVALUATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("evaluated"))
                .andExpect(jsonPath("$.flagKey").value("search_v2_experiment"));
    }

    @Test
    @DisplayName("Blank flagKey returns 400 Bad Request")
    void blankFlagKey_returns400() throws Exception {
        var request = new EvaluateFlagRequest("", "user_12345", 50.0, "control");
        mockMvc.perform(post(EVALUATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'flagKey')]").exists());
    }
    @Test
    @DisplayName("Null entityId returns 400 Bad Request")
    void nullEntityId_returns400() throws Exception {
        var request = new EvaluateFlagRequest("search_v2_experiment", null, 50.0, "control");
        mockMvc.perform(post(EVALUATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'entityId')]").exists());
    }
    @Test
    @DisplayName("Negative rollout percentage returns 400 Bad Request")
    void negativeRolloutPercentage_returns400() throws Exception {
        var request = new EvaluateFlagRequest("search_v2_experiment", "user_12345", -5.0, "control");
        mockMvc.perform(post(EVALUATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'rolloutPercentage')]").exists());
    }
    @Test
    @DisplayName("Rollout percentage exceeding 100 returns 400 Bad Request")
    void rolloutPercentageOver100_returns400() throws Exception {
        var request = new EvaluateFlagRequest("search_v2_experiment", "user_12345", 150.0, "control");
        mockMvc.perform(post(EVALUATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'rolloutPercentage')]").exists());
    }
    @Test
    @DisplayName("Variant name exceeding max size returns 400 Bad Request")
    void variantTooLong_returns400() throws Exception {
        String longVariant = "a".repeat(65);
        var request = new EvaluateFlagRequest("search_v2_experiment", "user_12345", 50.0, longVariant);
        mockMvc.perform(post(EVALUATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'defaultVariant')]").exists());
    }

}
