package com.intelliguard.service;

import com.intelliguard.entity.ModelDriftSnapshot;
import com.intelliguard.entity.ModelInferenceMetric;
import com.intelliguard.entity.ModelRegistryEntry;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.ModelDriftSnapshotRepository;
import com.intelliguard.repository.ModelInferenceMetricRepository;
import com.intelliguard.repository.ModelRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelGovernanceService {

    public static final String STATUS_CHAMPION = "CHAMPION";
    public static final String STATUS_CHALLENGER = "CHALLENGER";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    private final ModelRegistryRepository modelRegistryRepository;
    private final ModelInferenceMetricRepository metricRepository;
    private final ModelDriftSnapshotRepository driftSnapshotRepository;

    @Transactional
    public ModelRegistryEntry ensureChampionRegistered(String version, String artifactPath) {
        return modelRegistryRepository.findByVersion(version)
                .orElseGet(() -> modelRegistryRepository.save(ModelRegistryEntry.builder()
                        .version(version)
                        .status(STATUS_CHAMPION)
                        .artifactPath(artifactPath)
                        .trainingDataset("synthetic-fraud-training-v1")
                        .rocAuc(new BigDecimal("0.99290"))
                        .precisionScore(new BigDecimal("0.98100"))
                        .recallScore(new BigDecimal("0.97400"))
                        .promotedAt(LocalDateTime.now())
                        .build()));
    }

    @Transactional
    public void recordInference(Transaction transaction,
                                String modelVersion,
                                double score,
                                long latencyMs,
                                boolean fallback,
                                String errorMessage) {
        BigDecimal probability = score >= 0
                ? BigDecimal.valueOf(score).setScale(5, RoundingMode.HALF_UP)
                : null;
        metricRepository.save(ModelInferenceMetric.builder()
                .tenantId(transaction.getTenantId() != null ? transaction.getTenantId() : CurrentUserService.SYSTEM_TENANT)
                .transactionId(transaction.getId())
                .modelVersion(modelVersion)
                .fraudProbability(probability)
                .latencyMs(latencyMs)
                .fallback(fallback)
                .errorMessage(errorMessage)
                .build());
    }

    @Transactional
    public ModelDriftSnapshot createDriftSnapshot(String modelVersion, int windowMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        long sampleCount = metricRepository.countByModelVersionAndCreatedAtAfter(modelVersion, since);
        long fallbackCount = metricRepository.countByModelVersionAndFallbackAndCreatedAtAfter(modelVersion, true, since);
        long highRiskCount = metricRepository.countByModelVersionAndFraudProbabilityGreaterThanEqualAndCreatedAtAfter(
                modelVersion, new BigDecimal("0.75000"), since);
        BigDecimal avgScore = metricRepository.averageScore(modelVersion, since)
                .setScale(5, RoundingMode.HALF_UP);
        BigDecimal fallbackRate = ratio(fallbackCount, sampleCount);
        BigDecimal highRiskRate = ratio(highRiskCount, sampleCount);

        String status = "OK";
        String reason = "No drift signals exceeded thresholds";
        if (sampleCount == 0) {
            status = "INSUFFICIENT_DATA";
            reason = "No inference samples in window";
        } else if (fallbackRate.compareTo(new BigDecimal("0.05000")) > 0) {
            status = "ALERT";
            reason = "Model fallback rate exceeded 5%";
        } else if (highRiskRate.compareTo(new BigDecimal("0.40000")) > 0) {
            status = "WATCH";
            reason = "High-risk score rate exceeded 40%";
        }

        return driftSnapshotRepository.save(ModelDriftSnapshot.builder()
                .modelVersion(modelVersion)
                .windowMinutes(windowMinutes)
                .sampleCount(sampleCount)
                .avgScore(avgScore)
                .fallbackRate(fallbackRate)
                .highRiskRate(highRiskRate)
                .status(status)
                .reason(reason)
                .build());
    }

    public List<ModelRegistryEntry> listModels() {
        return modelRegistryRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ModelDriftSnapshot> recentDriftSnapshots(String modelVersion) {
        return driftSnapshotRepository.findTop20ByModelVersionOrderByCreatedAtDesc(modelVersion);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 5, RoundingMode.HALF_UP);
    }
}
