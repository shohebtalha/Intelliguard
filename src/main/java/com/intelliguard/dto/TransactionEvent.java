package com.intelliguard.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionEvent is what gets published to Kafka topics.
 *
 * Why a separate class from TransactionResponse?
 * Events are designed for async consumers — they need to be
 * self-contained with ALL context needed to process them
 * without making additional database calls.
 *
 * Rule: Events should be immutable and self-describing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {

    private String transactionId;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private String country;
    private String paymentMethod;
    private String deviceType;
    private String ipAddress;

    // Fraud decision fields
    private String status;           // APPROVE, REVIEW, BLOCK
    private BigDecimal fraudScore;
    private String flagReason;

    // Event metadata
    private String eventType;        // TXN_SUBMITTED, TXN_PROCESSED, FRAUD_ALERT
    private LocalDateTime eventTime;
    private Long decisionTimeMs;
}