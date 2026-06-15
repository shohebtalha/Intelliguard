package com.intelliguard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Who sent the money
    @Column(nullable = false)
    private String senderId;

    // Who received the money
    @Column(nullable = false)
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
    @Column(nullable = false, length = 50)
    private String paymentMethod;

    // Device used: MOBILE, DESKTOP, UNKNOWN
    @Column(length = 50)
    private String deviceType;

    // IP address of the sender
    @Column(length = 50)
    private String ipAddress;

    // Final decision: APPROVED, BLOCKED, REVIEW
    @Column(nullable = false, length = 20)
    private String status;

    // Fraud score from 0.0 (safe) to 1.0 (fraud)
    @Column(precision = 5, scale = 4)
    private BigDecimal fraudScore;

    // Which rule triggered (if any)
    @Column(length = 500)
    private String flagReason;

    // Auto-set when record is created
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}