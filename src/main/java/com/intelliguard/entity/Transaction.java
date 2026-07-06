package com.intelliguard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_sender_created", columnList = "sender_id, created_at"),
        @Index(name = "idx_transactions_status_created", columnList = "status, created_at"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_idempotency_key", columnNames = "idempotency_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;

    // Who sent the money
    @Column(name = "sender_id", nullable = false)
    private String senderId;

    // Who received the money
    @Column(name = "receiver_id", nullable = false)
    private String receiverId;

    // How much money
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Currency like INR, USD
    @Column(nullable = false, length = 10)
    private String currency;

    // Country where transaction happened
    @Column(nullable = false, length = 100)
    private String country;

    // Payment method: UPI, CARD, NET_BANKING
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    // Device used: MOBILE, DESKTOP, UNKNOWN
    @Column(name = "device_type", length = 50)
    private String deviceType;

    // IP address of the sender
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // Final decision: APPROVED, BLOCKED, REVIEW
    @Column(nullable = false, length = 20)
    private String status;

    // Fraud score from 0.0 (safe) to 1.0 (fraud)
    @Column(precision = 5, scale = 4)
    private BigDecimal fraudScore;

    // Which rule triggered (if any)
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "model_version", length = 80)
    private String modelVersion;

    // Auto-set when record is created
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;
}
