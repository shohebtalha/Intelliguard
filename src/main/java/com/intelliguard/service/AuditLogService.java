package com.intelliguard.service;

import com.intelliguard.entity.AuditLog;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AuditLogService creates a permanent record of every fraud decision.
 *
 * Key design decisions:
 *
 * 1. @Async — audit logging happens in a background thread.
 *    The HTTP response goes back to the client immediately.
 *    Audit log is written after. This keeps latency low.
 *
 * 2. Never throws exceptions — if audit logging fails,
 *    we LOG the failure but never let it affect the
 *    transaction response. Audit is important but not
 *    critical enough to fail the payment decision.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private static final String MODEL_VERSION = "xgboost-v1.0-onnx";

    /**
     * Create an audit log entry for a fraud decision.
     * Runs asynchronously — doesn't block the HTTP response.
     */
    @Async
    public void logDecision(Transaction transaction, Long decisionTimeMs) {
        try {
            // Get the currently logged-in user (from JWT token)
            String performedBy = getCurrentUsername();

            AuditLog auditLog = AuditLog.builder()
                    .transactionId(transaction.getId())
                    .senderId(transaction.getSenderId())
                    .receiverId(transaction.getReceiverId())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .country(transaction.getCountry())
                    .decision(transaction.getStatus())
                    .fraudScore(transaction.getFraudScore())
                    .flagReason(transaction.getFlagReason())
                    .modelVersion(MODEL_VERSION)
                    .decisionTimeMs(decisionTimeMs)
                    .performedBy(performedBy)
                    .build();

            auditLogRepository.save(auditLog);

            log.debug("Audit log created for transaction: {} decision: {}",
                    transaction.getId(), transaction.getStatus());

        } catch (Exception e) {
            // Never let audit logging failure affect the transaction
            log.error("Failed to create audit log for transaction {}: {}",
                    transaction.getId(), e.getMessage());
        }
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<AuditLog> getAuditLogsByTransaction(String transactionId) {
        return auditLogRepository.findByTransactionId(transactionId);
    }

    public List<AuditLog> getAuditLogsBySender(String senderId) {
        return auditLogRepository.findBySenderId(senderId);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "system";
    }
}