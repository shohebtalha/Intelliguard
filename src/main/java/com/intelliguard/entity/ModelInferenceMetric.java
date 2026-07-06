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
@Table(name = "model_inference_metrics", indexes = {
        @Index(name = "idx_model_metrics_version_created", columnList = "model_version, created_at"),
        @Index(name = "idx_model_metrics_tenant_created", columnList = "tenant_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelInferenceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Column(name = "fraud_probability", precision = 6, scale = 5)
    private BigDecimal fraudProbability;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(nullable = false)
    private boolean fallback;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
