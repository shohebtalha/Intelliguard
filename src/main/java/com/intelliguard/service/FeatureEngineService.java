package com.intelliguard.service;

import com.intelliguard.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

/**
 * FeatureEngineService converts a raw Transaction into a float[]
 * that the ONNX model can process.
 *
 * CRITICAL: The order of features MUST match exactly what was
 * used during Python training in train_model.py:
 *
 * f0: amount_normalized
 * f1: txn_count_10min
 * f2: amount_vs_avg_ratio
 * f3: is_high_risk_country
 * f4: is_night_time
 * f5: is_unknown_device
 * f6: account_age_days
 * f7: unique_countries_7days
 *
 * If you change the order here without retraining, the model
 * will give completely wrong predictions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureEngineService {

    private final VelocityService velocityService;

    // High-risk countries (same list as CountryBlocklistRule)
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "NG", "KP", "IR", "MM", "PK", "SY", "YE", "AF"
    );

    private static final Set<String> KNOWN_DEVICES = Set.of(
            "MOBILE", "DESKTOP", "TABLET"
    );

    // Max amount used for normalization (₹10 lakh)
    private static final double MAX_AMOUNT = 1_000_000.0;

    /**
     * Extract 8 features from a transaction and return as float[].
     * This is what gets fed directly into the ONNX model.
     */
    public float[] extractFeatures(Transaction transaction) {
        float[] features = new float[8];

        // f0: amount_normalized (0.0 to 1.0)
        double amount = transaction.getAmount().doubleValue();
        features[0] = (float) Math.min(amount / MAX_AMOUNT, 1.0);

        // f1: txn_count_10min — how many transactions in last 10 minutes
        VelocityService.VelocityMetrics metrics =
                velocityService.getMetrics(transaction.getSenderId());
        features[1] = (float) Math.min(metrics.getTxnCountLast10Min(), 30);

        // f2: amount_vs_avg_ratio — current amount vs sender's average
        features[2] = (float) computeAmountRatio(transaction);

        // f3: is_high_risk_country (0 or 1)
        String country = transaction.getCountry() != null
                ? transaction.getCountry().toUpperCase() : "";
        features[3] = HIGH_RISK_COUNTRIES.contains(country) ? 1.0f : 0.0f;

        // f4: is_night_time — between 1am and 5am (0 or 1)
        LocalTime now = LocalTime.now();
        boolean isNight = now.isAfter(LocalTime.of(1, 0))
                && now.isBefore(LocalTime.of(5, 0));
        features[4] = isNight ? 1.0f : 0.0f;

        // f5: is_unknown_device (0 or 1)
        String device = transaction.getDeviceType();
        boolean unknownDevice = device == null
                || device.isBlank()
                || device.equalsIgnoreCase("UNKNOWN")
                || !KNOWN_DEVICES.contains(device.toUpperCase());
        features[5] = unknownDevice ? 1.0f : 0.0f;

        // f6: account_age_days — we don't store this yet, use 180 as default
        // In production this would come from a user profile service
        features[6] = 180.0f;

        // f7: unique_countries_7days — approximated from velocity data
        // In production this would be a separate Redis counter
        features[7] = Math.min(metrics.getTxnCountLastHour() > 5 ? 3.0f : 1.0f, 8.0f);

        log.debug("Features for sender {}: amount_norm={} txn_count={} " +
                        "amount_ratio={} high_risk={} night={} unknown_device={}",
                transaction.getSenderId(),
                features[0], features[1], features[2],
                features[3], features[4], features[5]);

        return features;
    }

    /**
     * Compute ratio of current amount vs sender's recent average.
     * Returns 1.0 if no history (neutral — not suspicious).
     */
    private double computeAmountRatio(Transaction transaction) {
        VelocityService.VelocityMetrics metrics =
                velocityService.getMetrics(transaction.getSenderId());

        long txnCount = metrics.getTxnCountLastHour();
        if (txnCount <= 1) return 1.0; // no meaningful baseline

        BigDecimal totalAmount = metrics.getTotalAmountLastHour();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 1.0;
        }

        double avgAmount = totalAmount.doubleValue() / txnCount;
        if (avgAmount < 1.0) return 1.0;

        double ratio = transaction.getAmount().doubleValue() / avgAmount;
        return Math.min(ratio, 20.0); // cap at 20x
    }
}