package com.intelliguard.controller;

import com.intelliguard.dto.ApiResponse;
import com.intelliguard.service.MLScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom health endpoint that shows status of every component.
 * Goes beyond Spring's default /actuator/health by showing
 * ML model status, Redis status, and system info together.
 *
 * GET /api/health/status
 *
 * This is what you demo to interviewers:
 * "Every component has its own health check — if the ML model
 * fails to load, the system degrades gracefully to rules-only
 * mode instead of crashing."
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthController {

    private final MLScoringService mlScoringService;
    private final RedisTemplate<String, Long> redisTemplate;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Overall system info
        status.put("service", "IntelliGuard Fraud Detection Engine");
        status.put("version", "1.0.0");
        status.put("timestamp", LocalDateTime.now());

        // ML Model status
        Map<String, Object> ml = new LinkedHashMap<>();
        ml.put("loaded", mlScoringService.isModelLoaded());
        ml.put("version", "xgboost-v1.0-onnx");
        ml.put("rocAuc", "0.9929");
        ml.put("features", 8);
        ml.put("mode", mlScoringService.isModelLoaded() ? "ML + Rules (60/40)" : "Rules only");
        status.put("mlModel", ml);

        // Redis status
        Map<String, Object> redis = new LinkedHashMap<>();
        try {
            redisTemplate.opsForValue().get("health-check");
            redis.put("status", "UP");
            redis.put("purpose", "Velocity checks & sliding windows");
        } catch (Exception e) {
            redis.put("status", "DOWN");
            redis.put("error", e.getMessage());
        }
        status.put("redis", redis);

        // Fraud rules
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("count", 6);
        rules.put("active", new String[]{
                "AmountThresholdRule",
                "CountryBlocklistRule",
                "NightTimeRule",
                "UnknownDeviceRule",
                "VelocityRule",
                "AmountSpikeRule"
        });
        status.put("ruleEngine", rules);

        // System performance targets
        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("targetLatencyP99", "< 100ms");
        perf.put("decisionWeights", "60% ML + 40% Rules");
        perf.put("mlBlockThreshold", 0.75);
        perf.put("mlReviewThreshold", 0.45);
        status.put("performance", perf);

        return ResponseEntity.ok(ApiResponse.success(status, "System healthy"));
    }
}