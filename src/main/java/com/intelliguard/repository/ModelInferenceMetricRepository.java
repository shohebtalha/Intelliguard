package com.intelliguard.repository;

import com.intelliguard.entity.ModelInferenceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ModelInferenceMetricRepository extends JpaRepository<ModelInferenceMetric, String> {

    long countByModelVersionAndCreatedAtAfter(String modelVersion, LocalDateTime since);

    long countByModelVersionAndFallbackAndCreatedAtAfter(String modelVersion, boolean fallback, LocalDateTime since);

    long countByFallbackAndCreatedAtAfter(boolean fallback, LocalDateTime since);

    long countByModelVersionAndFraudProbabilityGreaterThanEqualAndCreatedAtAfter(
            String modelVersion, java.math.BigDecimal threshold, LocalDateTime since);

    @Query("""
            SELECT COALESCE(AVG(m.fraudProbability), 0)
            FROM ModelInferenceMetric m
            WHERE m.modelVersion = :modelVersion
              AND m.createdAt >= :since
              AND m.fraudProbability IS NOT NULL
            """)
    java.math.BigDecimal averageScore(String modelVersion, LocalDateTime since);
}
