package com.intelliguard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_cases", indexes = {
        @Index(name = "idx_cases_tenant_status_priority", columnList = "tenant_id, status, priority, created_at"),
        @Index(name = "idx_cases_assignee_status", columnList = "tenant_id, assigned_to, status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_cases_tenant_transaction", columnNames = {"tenant_id", "transaction_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "fraud_score", precision = 5, scale = 4)
    private BigDecimal fraudScore;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "resolution", length = 40)
    private String resolution;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;
}
