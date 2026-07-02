package com.intelliguard.controller;

import com.intelliguard.dto.ApiResponse;
import com.intelliguard.entity.Transaction;
import com.intelliguard.exception.TransactionNotFoundException;
import com.intelliguard.repository.TransactionRepository;
import com.intelliguard.service.FeatureEngineService;
import com.intelliguard.service.MLScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides SHAP-style explanations for fraud decisions.
 *
 * WHAT IS SHAP?
 * SHAP (SHapley Additive exPlanations) is a method to explain
 * ML model predictions. It tells you how much each feature
 * contributed to the final fraud score.
 *
 * Example output:
 * {
 *   "fraudScore": 0.94,
 *   "decision": "BLOCK",
 *   "explanation": {
 *     "is_high_risk_country": 0.31,   ← biggest contributor
 *     "txn_count_10min": 0.24,
 *     "is_unknown_device": 0.18,
 *     "amount_normalized": 0.14,
 *     "is_night_time": 0.07
 *   }
 * }
 *
 * This is the feature that impresses interviewers most —
 * it shows you understand ML in production, not just in notebooks.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ExplainabilityController {

    private final TransactionRepository transactionRepository;
    private final FeatureEngineService featureEngineService;
    private final MLScoringService mlScoringService;

    /**
     * GET /api/transactions/{id}/explain
     * Returns a SHAP-style breakdown of why this transaction was flagged.
     */
    @GetMapping("/{id}/explain")
    public ResponseEntity<ApiResponse<Map<String, Object>>> explainDecision(
            @PathVariable String id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found: " + id));

        // Get the raw feature values
        float[] features = featureEngineService.extractFeatures(transaction);

        // Feature names in the same order as training
        String[] featureNames = {
                "amount_normalized",
                "txn_count_10min",
                "amount_vs_avg_ratio",
                "is_high_risk_country",
                "is_night_time",
                "is_unknown_device",
                "account_age_days",
                "unique_countries_7days"
        };

        // Feature importance weights from our trained model
        // (from the feature importance output we saw when training)
        double[] featureImportance = {
                0.0093,  // amount_normalized
                0.0085,  // txn_count_10min
                0.0223,  // amount_vs_avg_ratio
                0.7477,  // is_high_risk_country ← dominant feature
                0.0755,  // is_night_time
                0.0738,  // is_unknown_device
                0.0207,  // account_age_days
                0.0421   // unique_countries_7days
        };

        // Compute SHAP-style contribution: feature_value × importance × fraud_score
        double fraudScore = transaction.getFraudScore() != null
                ? transaction.getFraudScore().doubleValue() : 0.0;

        Map<String, Object> shapValues = new LinkedHashMap<>();
        for (int i = 0; i < featureNames.length; i++) {
// Normalize account_age_days (feature index 6) to 0-1 range before computing contribution
            double featureVal = features[i];
            if (i == 6) featureVal = Math.min(featureVal / 2000.0, 1.0); // max 2000 days
            double contribution = featureVal * featureImportance[i] * fraudScore;            if (contribution > 0.001) { // only show meaningful contributions
                shapValues.put(featureNames[i],
                        Math.round(contribution * 10000.0) / 10000.0);
            }
        }

        // Sort by contribution descending (highest impact first)
        Map<String, Object> sortedShap = shapValues.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        (Double) b.getValue(), (Double) a.getValue()))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));

        // Build full explanation response
        Map<String, Object> explanation = new LinkedHashMap<>();
        explanation.put("transactionId", transaction.getId());
        explanation.put("fraudScore", transaction.getFraudScore());
        explanation.put("decision", transaction.getStatus());
        explanation.put("flagReason", transaction.getFlagReason());
        explanation.put("mlModelLoaded", mlScoringService.isModelLoaded());
        explanation.put("featureValues", buildFeatureMap(featureNames, features));
        explanation.put("shapContributions", sortedShap);
        explanation.put("topReason", sortedShap.isEmpty()
                ? "No significant fraud signals"
                : sortedShap.keySet().iterator().next()
                  .replace("_", " ") + " was the primary fraud signal");

        log.info("Explanation generated for transaction: {}", id);
        return ResponseEntity.ok(ApiResponse.success(explanation,
                "Explanation generated successfully"));
    }

    private Map<String, Object> buildFeatureMap(String[] names, float[] values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], Math.round(values[i] * 10000.0) / 10000.0);
        }
        return map;
    }
}