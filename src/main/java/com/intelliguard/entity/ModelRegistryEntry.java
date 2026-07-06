package com.intelliguard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_registry", indexes = {
        @Index(name = "idx_model_registry_status", columnList = "status, promoted_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_model_version", columnNames = "version")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelRegistryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String version;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "artifact_path", nullable = false, length = 500)
    private String artifactPath;

    @Column(name = "training_dataset", length = 200)
    private String trainingDataset;

    @Column(name = "roc_auc", precision = 6, scale = 5)
    private BigDecimal rocAuc;

    @Column(name = "precision_score", precision = 6, scale = 5)
    private BigDecimal precisionScore;

    @Column(name = "recall_score", precision = 6, scale = 5)
    private BigDecimal recallScore;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
