package com.intelliguard.dto;

import com.intelliguard.entity.Transaction;
import org.springframework.stereotype.Component;

/**
 * Mapper converts between:
 * TransactionRequest (what client sends) → Transaction (database entity)
 * Transaction (database entity) → TransactionResponse (what we send back)
 * This keeps our internal database structure hidden from the outside world.
 */
@Component
public class TransactionMapper {

    /**
     * Convert incoming request into a Transaction entity ready to save.
     * Notice: status, fraudScore, flagReason are NOT set here —
     * those get set by the fraud engine after analysis.
     */
    public Transaction toEntity(TransactionRequest request) {
        return Transaction.builder()
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .country(request.getCountry())
                .paymentMethod(request.getPaymentMethod())
                .deviceType(request.getDeviceType())
                .ipAddress(request.getIpAddress())
                .status("PENDING") // will be updated after fraud check
                .build();
    }

    /**
     * Convert saved Transaction entity into a response for the client.
     */
    public TransactionResponse toResponse(Transaction transaction, Long decisionTimeMs) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .senderId(transaction.getSenderId())
                .receiverId(transaction.getReceiverId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .country(transaction.getCountry())
                .paymentMethod(transaction.getPaymentMethod())
                .deviceType(transaction.getDeviceType())
                .status(transaction.getStatus())
                .fraudScore(transaction.getFraudScore())
                .flagReason(transaction.getFlagReason())
                .decisionTimeMs(decisionTimeMs)
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Overload for when we don't track decision time (e.g. fetching old records)
     */
    public TransactionResponse toResponse(Transaction transaction) {
        return toResponse(transaction, null);
    }
}