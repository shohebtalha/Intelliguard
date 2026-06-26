package com.intelliguard.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * This is what the CLIENT sends to us when submitting a transaction.
 * We never expose our Entity directly — DTOs act as a contract between
 * the outside world and our internal system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {

    @NotBlank(message = "Sender ID is required")
    private String senderId;

    @NotBlank(message = "Receiver ID is required")
    private String receiverId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "9999999999999.99", message = "Amount exceeds maximum limit")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code like INR or USD")
    private String currency;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String deviceType;

    private String ipAddress;
}