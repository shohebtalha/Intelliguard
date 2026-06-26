package com.intelliguard.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * This is what WE send back to the client after processing.
 * Notice it includes fraudScore, status, and flagReason —
 * things the client needs to know but didn't send.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private String id;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private String country;
    private String paymentMethod;
    private String deviceType;

    // The fraud decision
    private String status;           // APPROVED, BLOCKED, REVIEW
    private BigDecimal fraudScore;   // 0.0 = safe, 1.0 = definite fraud
    private String flagReason;       // Human-readable reason

    // How long did the decision take?
    private Long decisionTimeMs;

    private LocalDateTime createdAt;
}