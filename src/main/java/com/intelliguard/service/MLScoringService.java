package com.intelliguard.service;

import ai.onnxruntime.*;
import com.intelliguard.entity.Transaction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * MLScoringService loads the trained XGBoost model (as ONNX)
 * and runs fraud probability inference in pure Java.
 *
 * WHY ONNX RUNTIME IN JAVA?
 * - No Python process needed in production
 * - Inference runs in the same JVM — no network hop
 * - Adds only ~2ms to decision time
 * - Microsoft maintains ONNX Runtime — production grade
 *
 * @PostConstruct loads the model ONCE at startup.
 * All subsequent calls just run inference — fast.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MLScoringService {

    private final FeatureEngineService featureEngineService;

    private OrtEnvironment environment;
    private OrtSession session;
    private boolean modelLoaded = false;

    /**
     * Load model once at application startup.
     * If model file is missing, we log a warning and fall back
     * to rule-engine-only mode (graceful degradation).
     */
    @PostConstruct
    public void loadModel() {
        try {
            environment = OrtEnvironment.getEnvironment();

            // Load model.onnx from src/main/resources/ml/
            ClassPathResource resource = new ClassPathResource("ml/model.onnx");
            byte[] modelBytes = resource.getInputStream().readAllBytes();

            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = environment.createSession(modelBytes, options);

            modelLoaded = true;
            log.info("✅ ML model loaded successfully. Input: {} Output: {}",
                    session.getInputNames(),
                    session.getOutputNames());

        } catch (Exception e) {
            log.warn("⚠️  ML model not loaded: {}. Running rule-engine-only mode.", e.getMessage());
            modelLoaded = false;
        }
    }

    /**
     * Run ML inference for a transaction.
     * Returns fraud probability between 0.0 (safe) and 1.0 (fraud).
     *
     * If model isn't loaded, returns -1.0 (signals rule engine to ignore ML score).
     */
    public double predictFraudProbability(Transaction transaction) {
        if (!modelLoaded || session == null) {
            log.debug("ML model not available, skipping ML score");
            return -1.0;
        }

        try {
            // Step 1: Extract features
            float[] features = featureEngineService.extractFeatures(transaction);

            // Step 2: Create ONNX tensor — shape [1, 8] (1 sample, 8 features)
            long[] shape = {1, features.length};
            OnnxTensor inputTensor = OnnxTensor.createTensor(
                    environment,
                    new float[][]{features}
            );

            // Step 3: Run inference
            String inputName = session.getInputNames().iterator().next();
            OrtSession.Result result = session.run(
                    Collections.singletonMap(inputName, inputTensor)
            );

            // Step 4: Extract probability of class 1 (fraud)
            // Output is a Map<Long, Float> of {class_id -> probability}
            OnnxValue probabilitiesValue = result.get("probabilities").orElseThrow();
            @SuppressWarnings("unchecked")
            java.util.Map<Long, Float> probMap =
                    (java.util.Map<Long, Float>) probabilitiesValue.getValue();

            double fraudProbability = probMap.getOrDefault(1L, 0.0f).doubleValue();

            log.debug("ML fraud probability for sender {}: {}",
                    transaction.getSenderId(), fraudProbability);

            // Cleanup
            inputTensor.close();
            result.close();

            return fraudProbability;

        } catch (Exception e) {
            log.error("ML inference failed: {}", e.getMessage());
            return -1.0; // fallback to rules only
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) session.close();
            if (environment != null) environment.close();
            log.info("ML model session closed");
        } catch (Exception e) {
            log.warn("Error closing ML session: {}", e.getMessage());
        }
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }
}