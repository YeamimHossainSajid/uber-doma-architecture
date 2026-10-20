package com.uber.doma.domain_platform.experimentation_flipr_service.controller;

import com.uber.doma.domain_platform.experimentation_flipr_service.dto.EvaluateFlagRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/experiments")
public class ExperimentationFliprController {

    @PostMapping("/evaluate")
    public ResponseEntity<Map<String, Object>> evaluateFlag(@Valid @RequestBody EvaluateFlagRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", "evaluated",
                "flagKey", request.getFlagKey(),
                "entityId", request.getEntityId(),
                "variant", request.getDefaultVariant() != null ? request.getDefaultVariant() : "control"
        ));
    }
}
