package com.intelliguard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_drift_snapshots", indexes = {
        @Index(name = "idx_drift_model_created", columnList = "model_version, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelDriftSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Column(name = "window_minutes", nullable = false)
    private int windowMinutes;

    @Column(name = "sample_count", nullable = false)
    private long sampleCount;

    @Column(name = "avg_score", precision = 6, scale = 5)
    private BigDecimal avgScore;

    @Column(name = "fallback_rate", precision = 6, scale = 5)
    private BigDecimal fallbackRate;

    @Column(name = "high_risk_rate", precision = 6, scale = 5)
    private BigDecimal highRiskRate;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
