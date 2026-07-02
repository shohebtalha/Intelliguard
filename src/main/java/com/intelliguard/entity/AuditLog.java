package com.intelliguard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AuditLog is an IMMUTABLE record of every fraud decision.
 *
 * WHY AUDIT LOGS MATTER:
 * 1. Regulatory compliance — RBI requires banks to log all
 *    fraud decisions for 7 years
 * 2. Debugging — "why was this transaction blocked 3 months ago?"
 * 3. Model evaluation — "is our ML model getting better over time?"
 * 4. Legal protection — proof of due diligence if challenged
 *
 * KEY DESIGN: No update methods. Once created, never changed.
 * This is enforced by updatable=false on all columns.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_audit_sender_id", columnList = "senderId"),
        @Index(name = "idx_audit_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, updatable = false)
    private String transactionId;

    @Column(nullable = false, updatable = false)
    private String senderId;

    @Column(nullable = false, updatable = false)
    private String receiverId;

    @Column(nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private String currency;

    @Column(nullable = false, updatable = false)
    private String country;

    // The fraud decision
    @Column(nullable = false, updatable = false)
    private String decision; // APPROVE, BLOCK, REVIEW

    @Column(updatable = false, precision = 5, scale = 4)
    private BigDecimal fraudScore;

    @Column(length = 1000, updatable = false)
    private String flagReason;

    // ML model details at time of decision
    @Column(updatable = false)
    private String modelVersion;

    @Column(updatable = false)
    private Long decisionTimeMs;

    // Who was logged in when this decision was made
    @Column(updatable = false)
    private String performedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}